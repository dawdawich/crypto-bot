import React, {useEffect, useRef, useState} from "react";
import {Button, Slider, styled, Table, TableBody, TableCell, TableContainer, TableHead, TableRow} from "@mui/material";
import plexFont from "../../assets/fonts/IBM_Plex_Sans/IBMPlexSans-Regular.ttf";
import {useLocation} from "wouter";
import "../../css/pages/account/SubscriptionStyles.css";
import {RowDiv} from "../../utils/styles/element-styles";
import {useAuth} from "../../context/AuthContext";
import {TransactionResponse} from "../../model/TransactionResponse";
import {getAccountTransaction} from "../../service/AccountService";
import {UnauthorizedError} from "../../utils/errors/UnauthorizedError";
import {errorToast} from "../toast/Toasts";
import {getActiveAnalyzersCount} from "../../service/AnalyzerService";
import {formatDate} from "../../utils/date-utils";
import BuyTokensDialog from "./dialog/BuyTokensDialog";
import loadingSpinner from "../../assets/images/loading-spinner.svga";

const PreviousPath = styled('div')({
    font: plexFont,
    color: "#868F9C",
    fontSize: 20,
    fontWeight: '100',
    cursor: 'pointer'
});

const CurrentPath = styled('div')({
    font: plexFont,
    color: "white",
    fontSize: 20,
    fontWeight: '700',
    paddingLeft: '4px',
    userSelect: 'none',
    pointerEvents: 'none'
});

const Circle = styled('span')({
    width: '12px', height: '12px', borderRadius: '50%', marginRight: '8px'
});

const thirtyDaysInSeconds = 2_592_000;

const SubscriptionContent: React.FC = () => {
    const [animation, setAnimation] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const spanRef = useRef<HTMLSpanElement>(null);
    const {authInfo, logout} = useAuth();
    const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
    const [activeAnalyzersCount, setActiveAnalyzersCount] = useState(0);
    const [possibleAnalyzersCount, setPossibleAnalyzersCount] = useState(0);
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [now,] = useState(new Date());
    const [, navigate] = useLocation();

    if (!authInfo) {
        navigate("/analyzer");
    }
    document.title = 'Subscription';

    useEffect(() => {
        fetch(loadingSpinner)
            .then(response => response.text())
            .then(text => {
                setAnimation(text)
                if (spanRef.current) {
                    spanRef.current.innerHTML = animation
                }
            });
    }, [animation, isLoading]);

    useEffect(() => {
        getAccountTransaction(authInfo!)
            .then(response => setTransactions(response))
            .catch(ex => {
                if (ex instanceof UnauthorizedError) {
                    logout();
                }
                errorToast('Failed to get transactions');
            });

        getActiveAnalyzersCount(authInfo!)
            .then(count => setActiveAnalyzersCount(count))
            .catch(ex => {
                if (ex instanceof UnauthorizedError) {
                    logout();
                }
                errorToast('Failed to get active analyzers count');
            });
    }, [authInfo, logout]);

    useEffect(() => {
        const coinsSum = transactions
            .filter(transaction => (Math.floor(now.getTime() / 1000) - transaction.operationDate) < thirtyDaysInSeconds)
            .map(transaction => transaction.amount)
            .reduce((accumulator, currentValue) => accumulator + currentValue, 0);

        setPossibleAnalyzersCount(coinsSum * 100);
    }, [now, transactions]);

    const identifyStatus = (time: number) => {
        if ((Math.floor(now.getTime() / 1000) - time) < thirtyDaysInSeconds) {
            return (<div style={{color: '#16C079'}}>Active</div>);
        } else {
            return (<div style={{color: '#E7323B'}}>Expired</div>);
        }
    };
    
    const getNextMonth = (time: number) => formatDate(time + thirtyDaysInSeconds * 1000);

    return (
        <div className="account-subscription-content">
            <div className="account-subscription-header">
                <div className="account-subscription-header-path">
                    <PreviousPath onClick={() => navigate("/account")}>
                        Account /
                    </PreviousPath>
                    <CurrentPath>Subscription</CurrentPath>
                </div>
                <Button variant='contained'
                        onClick={() => setIsDialogOpen(true)}
                        style={{
                            textTransform: 'none',
                            backgroundColor: '#D0FF12',
                            color: '#121417',
                            fontWeight: 700
                        }}>
                    Buy more
                </Button>
            </div>
            <div className="account-subscription-header" style={{height: '18px'}}>
                <RowDiv style={{color: 'white'}}>
                    <Circle style={{backgroundColor: '#16C079'}}/>
                    Active Subscriptions
                </RowDiv>
                <RowDiv style={{color: 'white'}}>
                    <Circle style={{backgroundColor: '#868F9C'}}/>
                    Activated Analyzers {activeAnalyzersCount} / {possibleAnalyzersCount}
                </RowDiv>
            </div>
            <div style={{padding: '0 16px'}}>
                <Slider disabled min={0} max={possibleAnalyzersCount} value={activeAnalyzersCount}
                        sx={{
                            '.MuiSlider-thumb': {
                                display: 'none'
                            }
                        }}
                >

                </Slider>
            </div>
            <TableContainer>
                <Table>
                    <TableHead>
                        <TableRow className="token-table-headers">
                            <TableCell id="cell">Status</TableCell>
                            <TableCell id="cell">Amount</TableCell>
                            <TableCell id="cell">Subscription Started Date</TableCell>
                            <TableCell id="cell">Expiration Date</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {transactions
                            .sort((a, b) => b.operationDate - a.operationDate)
                            .map((transaction) => (
                            <TableRow key={transaction.operationDate}>
                                <TableCell id="cell">{identifyStatus(transaction.operationDate)}</TableCell>
                                <TableCell id="cell">{transaction.amount * 100} Analyzers</TableCell>
                                <TableCell id="cell">{formatDate(transaction.operationDate * 1000)}</TableCell>
                                <TableCell id="cell">{getNextMonth(transaction.operationDate * 1000)}</TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
            <BuyTokensDialog open={isDialogOpen} setIsLoading={setIsLoading} onClose={() => setIsDialogOpen(false)} />
            {isLoading &&
                <div style={{
                    position: 'absolute',
                    zIndex: 2300,
                    backgroundColor: 'rgba(0,0,0,0.5)',
                    height: '100%',
                    width: '100%',
                }}>
                    <span className="big-loading-banner" ref={spanRef} />
                </div>
            }
        </div>
    );
}

export default SubscriptionContent;
