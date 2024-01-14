import React, {JSX, useEffect, useState} from "react";
import {Analyzer} from "./model/Analyzer";
import {fetchAnalyzerData} from "../../service/AnalyzerService";
import {RouteComponentProps} from "wouter";
import {webSocketAnalyzerService} from "../../service/WebSocketService";
import {Box, Card, CardContent, Grid, Typography} from "@mui/material";
import PropertyCard from "./components/PropertyCard";
import AnalyzerProcessesChart from "./components/AnalyzerProcessesChart";
import {roundToNearest} from "../../utils/number-utils";

interface AnalyzerInfoPageProps extends RouteComponentProps<{ readonly analyzerId: string }> {
}

export interface ActiveAnalyzerInfo {
    id: string;
    orders: string[];
    currentPrice: number;
    middlePrice: number;
    positions: {
        long: boolean;
        size: number;
        entryPrice: number;
    }[];
}

const AnalyzerInfoPage: React.FC<AnalyzerInfoPageProps> = (props: AnalyzerInfoPageProps) => {
    const [analyzer, setAnalyzer] = useState<Analyzer>();
    const [analyzerInfo, setAnalyzerInfo] = useState<ActiveAnalyzerInfo>();
    const [analyzerFetchError, setAnalyzerFetchError] = useState<Error>();
    const [analyzerPositionInfo, setAnalyzerPositionInfo] = useState<JSX.Element[]>([]);

    useEffect(() => {
        fetchAnalyzerData(props.params.analyzerId)
            .then(data => setAnalyzer(data))
            .catch(error => setAnalyzerFetchError(error))
    }, [props.params.analyzerId]);

    useEffect(() => {
        if (!!analyzer && analyzer.isActive) {
            if (!webSocketAnalyzerService.isOpen()) {
                webSocketAnalyzerService.connect((analyzer) => {
                    setAnalyzerInfo(JSON.parse(analyzer));
                });
            }
            const intervalId = setInterval(() => {
                webSocketAnalyzerService.sendMessage(JSON.stringify({id: analyzer.id}));
            }, 5000);
            webSocketAnalyzerService.onDisconnect = () => {
                clearTimeout(intervalId);
            }

            if (!!analyzerInfo) {
                const position = analyzerInfo.positions.find((pos) => pos.size > 0);
                if (!!position) {
                    updatePositionInfoCard(position, analyzerInfo.currentPrice);
                }
            }
        }
    }, [analyzer, analyzerInfo, analyzerPositionInfo]);

    const updatePositionInfoCard = (position: {
        long: boolean;
        size: number;
        entryPrice: number;
    }, currentPrice: number) => {
        const profit = (position.long ? (currentPrice - position.entryPrice) : (position.entryPrice - currentPrice)) * position.size;
        setAnalyzerPositionInfo([
            <Grid key={'size'}>
                <PropertyCard title={'Position Size'} value={'' + roundToNearest(position.size, 0.001).toFixed(3)}/>
            </Grid>,
            <Grid key={'entry-price'}>
                <PropertyCard title={'Position Entry Price'} value={'' + roundToNearest(position.entryPrice, 0.1).toFixed(1)}/>
            </Grid>,
            <Grid key={'direction'}>
                <PropertyCard title={position.long ? 'Position Direction' : ''}
                              value={position.long ? 'Buy' : 'Sell'}/>
            </Grid>,
            <Grid key={'pnl'}>
                <PropertyCard title={'P&L'} value={'' + roundToNearest(profit, 0.1).toFixed(1)}/>
            </Grid>
        ]);
    }

    if (analyzerFetchError) return <div>Error: {analyzerFetchError.message}</div>
    if (!analyzer) return (
        <div>
            <h1>Analyzer Info Page</h1>
            Loading...
        </div>
    );

    return (
        <Box>
            <Typography variant="h4" component="h1" gutterBottom>
                Analyzer
            </Typography>
            <Typography color="textSecondary" gutterBottom>
                #{analyzer.id}
            </Typography>
            <Grid container spacing={3}>
                <Grid item xs={12} sm={6} md={3}>
                    <PropertyCard title={'Diapason'} value={analyzer.diapason + ' %'}/>
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                    <PropertyCard title={'Grid Size'} value={analyzer.gridSize.toString()}/>
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                    <PropertyCard title={'Multiplayer'} value={'x' + analyzer.multiplayer}/>
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                    <PropertyCard title={'Stop Loss'} value={analyzer.positionStopLoss + ' %'}/>
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                    <PropertyCard title={'Take Profit'} value={analyzer.positionTakeProfit + ' %'}/>
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                    <PropertyCard title={'Symbol'} value={analyzer.symbol}/>
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                    <PropertyCard title={'Money'} value={analyzer.money.toString()}/>
                </Grid>
            </Grid>
            {analyzer.isActive &&
                <Card>
                    <CardContent style={{display: 'flex'}}>
                        <AnalyzerProcessesChart info={analyzerInfo}/>
                        {analyzerPositionInfo}
                    </CardContent>
                </Card>
            }
        </Box>
    );
}

export default AnalyzerInfoPage;
