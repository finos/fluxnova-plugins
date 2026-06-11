package org.finos.fluxnova.bpm.engine.ai.agent.discovery.registry;

final class TransientException extends RuntimeException {
    TransientException(Exception cause) {
        super(cause);
    }
}
