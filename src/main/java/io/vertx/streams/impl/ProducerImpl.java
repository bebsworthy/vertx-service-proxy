/*
 * Copyright 2014 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */
package io.vertx.streams.impl;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.streams.CloseableReadStream;
import io.vertx.streams.Producer;
import io.vertx.streams.CloseableWriteStream;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ProducerImpl<T> implements Producer<T>, Handler<Message<Object>> {

  private final EventBus bus;
  private Handler<CloseableWriteStream<T>> readStreamHandler;
  private Handler<CloseableReadStream<T>> writeStreamHandler;
  private StreamProducerManager<T> mgr;

  public ProducerImpl(EventBus bus, Transport transport) {
    this.bus = bus;
    this.mgr = new StreamProducerManager<>(transport);
  }

  public ProducerImpl(EventBus bus) {
    this(bus, new EventBusTransport(bus));
  }

  @Override
  public Producer<T> readStreamHandler(Handler<CloseableWriteStream<T>> handler) {
    this.readStreamHandler = handler;
    return this;
  }

  @Override
  public Producer<T> writeStreamHandler(Handler<CloseableReadStream<T>> handler) {
    this.writeStreamHandler = handler;
    return this;
  }

  @Override
  public void handle(Message<Object> msg) {
    String streamAddress = msg.headers().get("addr");
    String action = msg.headers().get("stream");
    if (action != null) {
      switch (action) {
        case "open":
          mgr.openReadStream(streamAddress, ar -> {
            if (ar.succeeded()) {
              readStreamHandler.handle(ar.result());
              msg.reply(null);
            } else {
              // Something else ?
              msg.fail(0, ar.cause().getMessage());
            }
          });
          break;
        case "close":
          mgr.close(streamAddress);
          break;
      }
    }
  }

  @Override
  public void register(String address) {
    bus.consumer(address, this);
  }
}
