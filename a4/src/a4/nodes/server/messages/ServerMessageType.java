package a4.nodes.server.messages;

public enum ServerMessageType
{
    DATA,
    CHECK_IF_ALIVE,
    ALIVE_RESPONSE,
    MINOR_HEARTBEAT,
    MAJOR_HEARTBEAT
}
