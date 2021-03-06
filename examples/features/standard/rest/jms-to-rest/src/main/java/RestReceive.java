/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.spi.Link;

public class RestReceive {

   public static void main(String[] args) throws Exception {
      // first get the create URL for the shipping queue
      ClientRequest request = new ClientRequest("http://localhost:9095/queues/jms.queue.orders");
      ClientResponse res = request.head();
      Link pullConsumers = res.getHeaderAsLink("msg-pull-consumers");
      res = pullConsumers.request().formParameter("autoAck", "false").post();
      Link ackNext = res.getHeaderAsLink("msg-acknowledge-next");
      res.releaseConnection();
      while (true) {
         System.out.println("Waiting...");
         res = ackNext.request().header("Accept-Wait", "10").header("Accept", "application/xml").post();
         if (res.getStatus() == 503) {
            System.out.println("Timeout...");
            ackNext = res.getHeaderAsLink("msg-acknowledge-next");
         }
         else if (res.getStatus() == 200) {
            Order order = (Order) res.getEntity(Order.class);
            System.out.println(order);
            Link ack = res.getHeaderAsLink("msg-acknowledgement");
            res = ack.request().formParameter("acknowledge", "true").post();
            ackNext = res.getHeaderAsLink("msg-acknowledge-next");
         }
         else {
            throw new RuntimeException("Failure! " + res.getStatus());
         }
         res.releaseConnection();
      }
   }
}
