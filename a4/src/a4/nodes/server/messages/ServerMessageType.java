package a4.nodes.server.messages;

public enum ServerMessageType
{
    READ_DATA,
    ALIVE_RESPONSE,
    MINOR_HEARTBEAT,
    MAJOR_HEARTBEAT,
    RECOVERY_REQUEST,
    RECOVERY_DATA_REQUEST,
    RECOVERY_DATA,
    TRANSFER_DATA
}
