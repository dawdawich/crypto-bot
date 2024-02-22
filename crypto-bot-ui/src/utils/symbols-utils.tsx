import { ReactComponent as BTCUSDTIcon } from "../assets/images/analyzer/symbols/btcusdt-icon.svg";
import { ReactComponent as ETCUSDTIcon } from "../assets/images/analyzer/symbols/etcusdt-icon.svg";
import { ReactComponent as LINKUSDTIcon } from "../assets/images/analyzer/symbols/linkusdt-icon.svg";
import { ReactComponent as TIAUSDTIcon } from "../assets/images/analyzer/symbols/tiausdt-icon.svg";
import { ReactComponent as XRPUSDTIcon } from "../assets/images/analyzer/symbols/xrpusdt-icon.svg";
import { ReactComponent as DOGEUSDTIcon } from "../assets/images/analyzer/symbols/dogeusdt-icon.svg";
import { ReactComponent as ETHUSDTIcon } from "../assets/images/analyzer/symbols/ethusdt-icon.svg";
import { ReactComponent as ORDIUSDTIcon } from "../assets/images/analyzer/symbols/ordiusdt-icon.svg";
import { ReactComponent as SOLUSDTIcon } from "../assets/images/analyzer/symbols/solusdt-icon.svg";
import {JSX} from "react";

const analyzers = {
    "BTCUSDT": <BTCUSDTIcon />,
    "ETCUSDT": <ETCUSDTIcon />,
    "LINKUSDT": <LINKUSDTIcon />,
    "TIAUSDT": <TIAUSDTIcon />,
    "XRPUSDT": <XRPUSDTIcon />,
    "DOGEUSDT": <DOGEUSDTIcon />,
    "ETHUSDT": <ETHUSDTIcon />,
    "ORDIUSDT": <ORDIUSDTIcon />,
    "SOLUSDT": <SOLUSDTIcon />,
};

export const getSymbolIcon = (name: string): JSX.Element => {
    return (analyzers as any)[name] as JSX.Element;
};
