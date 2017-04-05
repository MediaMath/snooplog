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
