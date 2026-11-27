package com.carland.carland_service.service;

import com.carland.carland_service.exceptions.MissingFieldException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatAudioProbeTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void aacInM4aIsAcceptedAndCoverArtIsIgnored() throws Exception {
        String report = """
                {
                  "streams": [
                    {"codec_name": "aac", "codec_type": "audio", "duration": "4.5"},
                    {"codec_name": "mjpeg", "codec_type": "video", "disposition": {"attached_pic": 1}}
                  ],
                  "format": {"format_name": "mov,mp4,m4a,3gp,3g2,mj2", "duration": "4.501"}
                }
                """;
        assertEquals(4501L, ChatAudioProbe.readReport(JSON.readTree(report)).durationMs());
    }

    @Test
    void mp3AndRealVideoAndOvertimeAreRejected() throws Exception {
        assertThrows(MissingFieldException.class, () -> ChatAudioProbe.readReport(JSON.readTree("""
                {"streams":[{"codec_name":"mp3","codec_type":"audio"}],
                 "format":{"format_name":"mp3","duration":"3"}}
                """)));
        assertThrows(MissingFieldException.class, () -> ChatAudioProbe.readReport(JSON.readTree("""
                {"streams":[
                   {"codec_name":"aac","codec_type":"audio"},
                   {"codec_name":"h264","codec_type":"video","disposition":{"attached_pic":0}}
                 ],
                 "format":{"format_name":"mov,mp4,m4a,3gp,3g2,mj2","duration":"3"}}
                """)));
        MissingFieldException tooLong = assertThrows(MissingFieldException.class, () -> ChatAudioProbe.readReport(JSON.readTree("""
                {"streams":[{"codec_name":"aac","codec_type":"audio"}],
                 "format":{"format_name":"mp4","duration":"301"}}
                """)));
        assertEquals("audioTooLong", tooLong.getMessage());
    }

    @Test
    void browserVoiceCanBeConvertedAndVideoCannot() throws Exception {
        assertTrue(ChatAudioProbe.convertibleVoice(JSON.readTree("""
                {"streams":[{"codec_name":"opus","codec_type":"audio"}],
                 "format":{"format_name":"matroska,webm","duration":"3.2"}}
                """)));
        assertFalse(ChatAudioProbe.convertibleVoice(JSON.readTree("""
                {"streams":[
                   {"codec_name":"aac","codec_type":"audio"},
                   {"codec_name":"h264","codec_type":"video"}
                 ],
                 "format":{"format_name":"mov,mp4,m4a,3gp,3g2,mj2","duration":"3"}}
                """)));
    }

    @Test
    void clientDurationStandsOnlyWhenItMatchesProbe() {
        assertEquals(11_500L, ChatAudioProbe.resolveDuration(11_500L, 12_000L));
        assertEquals(12_000L, ChatAudioProbe.resolveDuration(1_000L, 12_000L));
        assertEquals(12_000L, ChatAudioProbe.resolveDuration(null, 12_000L));
    }

    @Test
    void rangeReturns206And416() {
        byte[] data = {0, 1, 2, 3};

        ChatAudioProbe.Slice full = ChatAudioProbe.slice(data, null);
        assertEquals(200, full.status());
        assertEquals(4, full.body().length);
        assertNull(full.contentRange());

        ChatAudioProbe.Slice head = ChatAudioProbe.slice(data, "bytes=0-1");
        assertEquals(206, head.status());
        assertEquals(2, head.body().length);
        assertEquals(0, head.body()[0]);
        assertEquals(1, head.body()[1]);
        assertEquals("bytes 0-1/4", head.contentRange());

        ChatAudioProbe.Slice tail = ChatAudioProbe.slice(data, "bytes=2-");
        assertEquals(206, tail.status());
        assertEquals("bytes 2-3/4", tail.contentRange());

        ChatAudioProbe.Slice suffix = ChatAudioProbe.slice(data, "bytes=-1");
        assertEquals(206, suffix.status());
        assertEquals("bytes 3-3/4", suffix.contentRange());
        assertEquals(3, suffix.body()[0]);

        ChatAudioProbe.Slice missing = ChatAudioProbe.slice(data, "bytes=4-5");
        assertEquals(416, missing.status());
        assertEquals("bytes */4", missing.contentRange());
    }
}
