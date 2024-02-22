import {WEB_SOCKET_HOST} from "./Constants";

type MessageHandler = (message: string) => void;

export class WebSocketService {
    private socket: WebSocket | null = null;
    private url: string;
    public onDisconnect = () => {};

    constructor(url: string) {
        this.url = url;
    }

    public connect(onMessage: MessageHandler): void {
        this.socket = new WebSocket(this.url);

        this.socket.onopen = () => {
            console.log('WebSocket Connected');
        };

        this.socket.onmessage = (event) => {
            onMessage(event.data);
        };

        this.socket.onclose = () => {
            console.log('WebSocket Disconnected');
            this.onDisconnect();
            // Optionally try to reconnect or handle disconnection
        };

        this.socket.onerror = (error) => {
            console.error('WebSocket Error: ', error);
            this.onDisconnect();
        };
    }

    public disconnect(): void {
        if (this.socket) {
            this.socket.close();
        }
    }

    public isOpen(): boolean {
        return this.socket !== null;
    }

    public sendMessage(message: string): void {
        if (this.socket) {
            this.socket.send(message);
        }
    }
}

export const getWebSocketAnalyzerService = () => new WebSocketService(`${WEB_SOCKET_HOST}/ws/analyzer`);
export const webSocketManagerService = new WebSocketService(`${WEB_SOCKET_HOST}/ws/manager`);
