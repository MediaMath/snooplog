package main

import (
	"flag"
	"fmt"
	zmq "github.com/pebbe/zmq4"
	"log"
	"time"
)

func main() {
	var hostport = flag.String("hostport", "localhost:5557", "Zmq PUB endpoint")
	flag.Parse()
	sock, _ := zmq.NewSocket(zmq.SUB)
	ep := fmt.Sprintf("tcp://%s", *hostport)
	if err := sock.Connect(ep); err != nil {
		log.Panic(err)
	}
	sock.SetSubscribe("")
	poller := zmq.NewPoller()
	poller.Add(sock, zmq.POLLIN)
	for {
		if polled, err := poller.Poll(time.Duration(1) * time.Second); err == nil {
			if len(polled) > 0 {
				msg, err := polled[0].Socket.Recv(0)
				if err != nil {
					log.Panic(err)
				}
				fmt.Println(msg)
			}
		} else {
			log.Println(err)
		}

	}
}
