import React, {useEffect, useRef, useState} from "react";
import {Button, Dialog, DialogActions, DialogContent, DialogTitle, styled} from "@mui/material";
import {AuthInfo} from "../../../model/AuthInfo";
import {FolderModel} from "../../../model/FolderModel";
import {ReactComponent as CrossIcon} from '../../../assets/images/action-icon/cross-icon.svg';
import loadingSpinner from "../../../assets/images/loading-spinner.svga";
import {addAnalyzersToFolder, createFolder, deleteFolder, renameFolder} from "../../../service/FolderService";
import {errorToast, successToast} from "../../toast/Toasts";
import "../../../css/pages/analyzer/dialog/FolderDialogStyles.css"
import {SelectStyle} from "../../../utils/styles/element-styles";
import Select, {ActionMeta} from "react-select";

export type FolderActionType = 'create' | 'rename' | 'delete' | 'addToFolder';

const FieldContainer = styled('div')({
    display: 'flex',
    flexDirection: 'column'
});

interface FolderDialogProps {
    actionType: FolderActionType;
    authInfo: AuthInfo;
    currentFolder: FolderModel | null;
    currentFolderList: FolderModel[];
    analyzerIds: string[],
    open: boolean;
    onClose: () => void;
    onCreate: (folder: FolderModel) => void;
    onDelete: (folder: FolderModel) => void;
    onRename: (folder: FolderModel) => void;
}

export const FolderDialog: React.FC<FolderDialogProps> = ({
                                                              actionType,
                                                              authInfo,
                                                              currentFolder,
                                                              currentFolderList,
                                                              analyzerIds,
                                                              open,
                                                              onClose,
                                                              onCreate,
                                                              onDelete,
                                                              onRename
                                                          }) => {
    const [choosedFolder, setChoosedFolder] = useState<FolderModel | null>();
    const [nameText, setNameText] = useState('');
    const spanRef = useRef<HTMLSpanElement>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [animation, setAnimation] = useState('');

    useEffect(() => {
        if (!!currentFolder) {
            setNameText(currentFolder.name);
        }
    }, [currentFolder]);

    useEffect(() => {
        if (isLoading) {
            fetch(loadingSpinner)
                .then(response => response.text())
                .then(text => {
                    setAnimation(text)
                    if (spanRef.current) {
                        spanRef.current.innerHTML = animation
                    }
                });
        }
    }, [animation, isLoading]);

    const handleSubmit = () => {
        switch (actionType) {
            case "create":
                setIsLoading(true);
                createFolder(authInfo, nameText)
                    .then((folder) => {
                        setIsLoading(false);
                        setNameText('');
                        onCreate(folder);
                        onClose();
                        successToast(`Folder '${nameText}' created successfully.`);
                    })
                    .catch(() => {
                        setIsLoading(false);
                        errorToast(`Failed to create folder '${nameText}'.`);
                    });
                break;
            case "delete":
                if (!!currentFolder) {
                    setIsLoading(true);
                    deleteFolder(authInfo, currentFolder.id)
                        .then(() => {
                            setIsLoading(false);
                            onDelete(currentFolder);
                            onClose();
                            successToast(`Folder '${currentFolder.name}' deleted successfully.`);
                        })
                        .catch(() => {
                            setIsLoading(false);
                            errorToast(`Failed to delete folder '${currentFolder.name}'.`);
                        });
                }
                break;
            case "rename":
                if (!!currentFolder) {
                    setIsLoading(true);
                    renameFolder(authInfo, currentFolder.id, nameText)
                        .then((result) => {
                            setIsLoading(false);
                            if (result) {
                                onRename({id: currentFolder.id, name: nameText});
                                onClose();
                                successToast(`Folder renamed successfully.`);
                            } else {
                                errorToast(`Failed to rename folder . Folder with the same name already exists.`);
                            }
                        })
                        .catch(() => {
                            setIsLoading(false);
                            errorToast(`Failed to rename folder '${currentFolder.name}'.`);
                        });
                }
                break;
            case "addToFolder":
                if (!!choosedFolder && analyzerIds.length > 0) {
                    setIsLoading(true);
                    addAnalyzersToFolder(authInfo, choosedFolder.id, analyzerIds)
                        .then(() => {
                            setIsLoading(false);
                            onClose();
                            successToast(`Successfully added analyzer(s) to folder.`);
                        })
                        .catch(() => {
                            setIsLoading(false);
                            errorToast(`Failed to add analyzer(s) to folder.`);
                        });
                }
        }
    };

    const selectedFolderChange = (option: any, actionMeta: ActionMeta<unknown>) => {
        setChoosedFolder({id: option.value, name: option.label});
    };

    let title = '';
    let description = '';

    let actionButtonText = ''

    switch (actionType) {
        case "create":
            title = 'Create Folder';
            description = 'Folder name';
            actionButtonText = 'Create';
            break;
        case "rename":
            title = 'Rename Folder';
            description = 'Folder name';
            actionButtonText = 'Rename';
            break;
        case "delete":
            title = 'Delete Folder';
            description = 'Are you sure?';
            actionButtonText = 'Delete';
            break;
        case "addToFolder":
            title = 'Add to the Folder';
            description = 'Analyzer folder';
            actionButtonText = 'Add';
            break;
    }

    return (
        <Dialog open={open} onClose={onClose} aria-labelledby="form-dialog-title"
                PaperProps={{
                    style: {
                        backgroundColor: '#121417',
                        borderRadius: '4px',
                        boxShadow: 'none',
                        color: 'white',
                        fontWeight: '400',
                        width: '380px',
                        height: '280px',
                        position: 'relative',
                        overflow: 'visible'
                    }
                }}
        >
            <CrossIcon style={{
                display: 'flex',
                alignSelf: 'flex-end',
                fill: 'white',
                width: '24px',
                height: '24px',
                marginRight: '16px',
                marginTop: '16px',
                cursor: 'pointer'
            }} onClick={onClose}/>
            <DialogTitle>
                {title}
            </DialogTitle>
            <DialogContent style={{ overflow: 'visible'}}>
                <FieldContainer style={{marginTop: '16px', overflow: 'visible'}}>
                    <div style={{fontSize: '14px', fontWeight: '200'}}>
                        {description}
                    </div>
                    {
                        actionType !== 'delete' &&
                        actionType === 'addToFolder' ?
                            <Select
                                options={currentFolderList.map((folder) => {
                                    return {value: folder.id, label: folder.name}
                                })}
                                components={{IndicatorSeparator: () => null}}
                                placeholder=""
                                name="folder"
                                onChange={selectedFolderChange}
                                styles={SelectStyle}
                            />
                            :
                        <input type="text" name="name" style={{
                            borderRadius: '4px',
                            height: '34px',
                            boxShadow: 'none',
                            border: 0,
                            backgroundColor: '#262B31',
                            color: 'white',
                            padding: '8px',
                            fontSize: '14px',
                            fontWeight: '200',
                        }}
                               value={nameText}
                               onChange={(e) => setNameText(e.target.value)}/>}
                </FieldContainer>
            </DialogContent>
            <DialogActions style={{paddingBottom: '32px', paddingRight: '32px'}}>
                <Button variant='outlined' style={{textTransform: 'none'}} onClick={onClose} color="error">
                    Cancel
                </Button>
                <Button variant={'contained'} onClick={handleSubmit}
                        style={{
                            textTransform: 'none',
                            backgroundColor: actionType !== 'delete' ? '#D0FF12' : 'red',
                            color: '#121417'
                        }}>
                    {actionButtonText}
                </Button>
            </DialogActions>
            {isLoading &&
                <div style={{
                    position: 'absolute',
                    zIndex: 4,
                    backgroundColor: 'rgba(0,0,0,0.5)',
                    width: '100%',
                    height: '100%',
                    alignSelf: "center"
                }}>
                    <span className="dialog-loading-banner" ref={spanRef}/>
                </div>
            }
        </Dialog>
    );
}

export default FolderDialog;
