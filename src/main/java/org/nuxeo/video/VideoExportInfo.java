package org.nuxeo.video;

import java.io.Serializable;

public class VideoExportInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    Long height;

    Double fps;

    String extension;

    String mimeType;

    String acodec;

    String vcodec;

    String profile;

    String target;

    public VideoExportInfo(Long height, Double fps, String extension,
            String mimeType, String acodec, String vcodec, String profile,
            String target) {
        this.height = height;
        this.fps = fps;
        this.extension = extension;
        this.mimeType = mimeType;
        this.acodec = acodec;
        this.vcodec = vcodec;
        this.profile = profile;
        this.target = target;
    }

    public String getTitle() {
        return target + " " + profile;
    }

}
