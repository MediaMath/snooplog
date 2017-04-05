/**
* Copyright (C) 2017 MediaMath <http://www.mediamath.com>
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
* @author cresnick
 */
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
