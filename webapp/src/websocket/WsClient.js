import { BehaviorSubject } from 'rxjs';
import { WebSocketSubject } from 'rxjs/webSocket'
// import { from } from 'rxjs';
// import { switchMapTo } from 'rxjs/operators';
import { map, takeWhile, filter, take, distinctUntilChanged } from 'rxjs/operators';
import { ConnectionStates } from '@/websocket/ClientUtils.js'
import { Messages, Code } from '@/websocket/ClientUtils.js'
//________________________________________________________________________________
// WsClient
// ________________________________________________________________________________
export default class WsClient {

    constructor() {
        this.status$ = new BehaviorSubject(ConnectionStates.IDLE);
        this.url = null
        this.socket$ = null
        this.logger = () => {}
    }

    status() {
        return this.status$.pipe(distinctUntilChanged())
    }

    connect(url) {
        this.url = url
        this.status$.next(ConnectionStates.CONNECTING)
        return new Promise((resolve, reject) => {
            // https://github.com/ReactiveX/rxjs/blob/master/src/internal/observable/dom/WebSocketSubject.ts
            this.socket$ = new WebSocketSubject({
                url: this.url,
                deserializer: msg => msg,
                openObserver: {
                    next: () => {
                        this.status$.next(ConnectionStates.CONNECTED)
                        resolve(true)
                    },
                    error: e => reject(e)
                },
                closeObserver: {
                    next: () => this.status$.next(ConnectionStates.CLOSED),
                    error: e => console.log(e)
                }
            });

            this.socket$.subscribe(
                (data) => this.logger(data.data),
                (error) => {
                    if (error.type === "close") {
                        console.log("got close event")
                    }
                    if (error.type === "error") {
                        this.status$.next(ConnectionStates.ERROR)
                        reject(Messages.CONNECT_FAILED)
                    }
                }
            )
        });
    }

    close() {
        this.socket$.unsubscribe()
    }

    send(aWsPacket) {
        this.socket$.next(aWsPacket)
    }

    sendAndGetPromise(aWsPacket) {
        const promise = this.socket$.pipe(
            map(event => event.data),
            map(data => JSON.parse(data)),
            filter(reply => {
                //console.log(replyPkt)
                // console.log(`check ${replyPkt.id} == ${sendPkt.id}`)
                //console.log(`answer is ${replyPkt.id == sendPkt.id}`)

                return reply.id == aWsPacket.id
            }),
            take(1)
        ).toPromise();
        this.socket$.next(aWsPacket)
        return promise;
    }

    sendAndGetObservable(aWsPacket) {
        this.socket$.next(aWsPacket)
        return this.socket$.pipe(
            map(event => event.data),
            map(data => JSON.parse(data)),
            filter(reply => {
                return reply.id == aWsPacket.id
            }),
            takeWhile( reply => reply.code !== Code.LACK , true)
        )

    }



}