package com.itmecasey.barelyquesting.core.net;

/** Message ids. Server to client are below 20, client to server from 20. */
public final class Msg {
    /** book chunk: index, total, gzip bytes. */
    public static final int S_BOOK = 1;
    /** progress: team id, json. */
    public static final int S_PROGRESS = 2;
    /** one edit operation: sender uuid, op json. */
    public static final int S_OP = 3;
    /** team info json. */
    public static final int S_TEAM = 4;
    /** toast: title, text. */
    public static final int S_TOAST = 5;
    /** open the book (optionally a quest id). */
    public static final int S_OPEN = 6;
    /** player info: uuid, can edit. */
    public static final int S_INFO = 7;
    /** a message for the player: kind (info, error), text. */
    public static final int S_RESULT = 8;

    public static final int C_HELLO = 20;
    public static final int C_CLAIM = 21;
    public static final int C_SUBMIT = 22;
    public static final int C_CHECK = 23;
    public static final int C_EDIT = 24;
    public static final int C_TEAM = 25;

    /** Largest chunk of book data in one message (the game limits server to client payloads to about 1 MiB). */
    public static final int CHUNK = 200_000;

    private Msg() {
    }
}
