package main

import (
	"flag"
	zmq "github.com/pebbe/zmq4"
	"log"
)

// Run-forever go-between process for logged messages and clients

func snoopProxy() {
	var fp = flag.String("f", "5556", "frontend port for logger connect endpoint")
	var bp = flag.String("b", "5557", "backend port for client connect endpoint")
	frontend, err := zmq.NewSocket(zmq.XSUB)
	if err != nil {
		log.Fatal(err)
	}
	defer frontend.Close()
	frontend.Bind("tcp://*:" + *fp)

	backend, err := zmq.NewSocket(zmq.XPUB)
	if err != nil {
		log.Fatal(err)
	}
	defer backend.Close()
	backend.Bind("tcp://*:" + *bp)

	err = zmq.Proxy(frontend, backend, nil)
	log.Fatalln("Proxy interrupted:", err)
}

func main() {
	snoopProxy()
}
