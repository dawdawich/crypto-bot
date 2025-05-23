import {TableCell, TableRow} from "@mui/material";
import React from "react";
import "../css/shared/LoadingTableRows.css";

export type LoadingTableConfig = {
    rows: number;
    columns: number;
    prefixSkipColumns?: number;
    postfixSkipColumns?: number;
}

const loadingTableRows = (params: LoadingTableConfig) => {
    return Array.from({length: params.rows}, (_, index) => {
        return (
            <TableRow key={'loading-row-' + index}>
                {params.prefixSkipColumns && Array.from({length: params.prefixSkipColumns}, (_, cellIndex) => (
                    <TableCell id="load-cell" key={'prefix-loading-cell-' + index + '-' + cellIndex} />))}
                {Array.from({length: params.columns}, (_, cellIndex) => (
                    <TableCell id="load-cell" key={'loading-cell-' + index + '-' + cellIndex}>
                        <div className="row-loading"/>
                    </TableCell>))}
                {params.postfixSkipColumns && Array.from({length: params.postfixSkipColumns}, (_, cellIndex) => (
                    <TableCell id="cell" key={'postfix-loading-cell-' + index + '-' + cellIndex} />))}
            </TableRow>
        );
    });
}

export default loadingTableRows;
