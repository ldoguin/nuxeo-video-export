package org.nuxeo.video;

import static org.nuxeo.video.ExportedVideoConstants.EXPORTED_VIDEOS_PROPERTY;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;

import com.google.common.collect.Maps;

@Name("videoExportActions")
@Install(precedence = Install.FRAMEWORK)
public class VideoExportActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    private Map<String, ExportedTranscodedVideo> exportedVideos;

    public String getTranscodedVideoURL(DocumentModel doc,
            ExportedTranscodedVideo transcodedVideo) {
        String blobPropertyName = transcodedVideo.getBlobPropertyName();
        return DocumentModelFunctions.bigFileUrl(doc, blobPropertyName,
                transcodedVideo.getBlob().getFilename());
    }

    public void removeExport(DocumentModel doc,
            ExportedTranscodedVideo transcodedVideo) throws PropertyException,
            ClientException {
        List<Map<String, Serializable>> videos = (List<Map<String, Serializable>>) doc.getPropertyValue(EXPORTED_VIDEOS_PROPERTY);
        videos.remove(transcodedVideo.getPosition());
        doc.setPropertyValue(EXPORTED_VIDEOS_PROPERTY, (Serializable) videos);
        documentManager.saveDocument(doc);
    }

    public Collection<ExportedTranscodedVideo> getExportedVideos(
            DocumentModel doc) {
        if (exportedVideos == null) {
            initExportedVideos(doc);
        }
        return exportedVideos.values();
    }

    private void initExportedVideos(DocumentModel doc) {
        try {
            if (exportedVideos == null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Serializable>> videos = (List<Map<String, Serializable>>) doc.getPropertyValue(EXPORTED_VIDEOS_PROPERTY);
                exportedVideos = Maps.newHashMap();
                for (int i = 0; i < videos.size(); i++) {
                    ExportedTranscodedVideo transcodedVideo = ExportedTranscodedVideo.fromMapAndPosition(
                            videos.get(i), i);
                    exportedVideos.put(transcodedVideo.getName(),
                            transcodedVideo);
                }
            }
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }
}
