package org.nuxeo.video;

import static org.nuxeo.ecm.core.api.CoreSession.ALLOW_VERSION_WRITE;
import static org.nuxeo.video.ExportedVideoConstants.EXPORTED_VIDEOS_PROPERTY;
import static org.nuxeo.video.ExportedVideoConstants.VIDEO_EXPORT_FACET;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.video.TranscodedVideo;
import org.nuxeo.ecm.platform.video.Video;
import org.nuxeo.ecm.platform.video.VideoHelper;
import org.nuxeo.ecm.platform.video.VideoInfo;
import org.nuxeo.ecm.platform.video.service.VideoConversionWork;
import org.nuxeo.runtime.api.Framework;

public class CustomVideoConversionWork extends VideoConversionWork {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(CustomVideoConversionWork.class);

    public static final String CATEGORY_VIDEO_CONVERSION = "videoConversion";

    public static final String VIDEO_CONVERSIONS_DONE_EVENT = "videoConversionsDone";

    protected final Properties properties;

    protected final VideoExportInfo vei;

    protected final String nodeId;

    protected final String workflowInstanceId;

    public CustomVideoConversionWork(String workflowInstanceId, String nodeId,
            String repositoryName, String docId, VideoExportInfo vei,
            Properties properties) {
        super(repositoryName, docId, vei.getTitle());
        setDocument(repositoryName, docId);
        this.properties = properties;
        this.vei = vei;
        this.nodeId = nodeId;
        this.workflowInstanceId = workflowInstanceId;
    }

    @Override
    public String getTitle() {
        return "Video Conversion " + vei.getTitle();
    }

    @Override
    public void work() throws Exception {
        setStatus("Extracting");
        setProgress(Progress.PROGRESS_INDETERMINATE);

        Video originalVideo = null;
        try {
            initSession();
            originalVideo = getVideoToConvert();
            commitOrRollbackTransaction();
        } finally {
            cleanUp(true, null);
        }

        if (originalVideo == null) {
            setStatus("Nothing to process");
            return;
        }

        // Perform the actual conversion
        setStatus("Transcoding");
        TranscodedVideo transcodedVideo = convert(originalVideo);

        // Saving it to the document
        startTransaction();
        setStatus("Saving");
        initSession();
        DocumentModel doc = session.getDocument(new IdRef(docId));
        saveNewTranscodedVideo(doc, transcodedVideo);
        fireVideoConversionsDoneEvent(doc);
        setStatus("Done");
        TaskService taskService = Framework.getLocalService(TaskService.class);
        List<Task> tasks = taskService.getAllTaskInstances(workflowInstanceId,
                nodeId, session);
        Task task = tasks.get(0);
        task.getDocument().attach(session.getSessionId());
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("comment", "The video export has been completed.");
        DocumentRoutingService documentRoutingService = Framework.getLocalService(DocumentRoutingService.class);
        documentRoutingService.endTask(session, task, params, "ExportDone");
    }

    public TranscodedVideo convert(Video originalVideo) {
        try {
            BlobHolder blobHolder = new SimpleBlobHolder(
                    originalVideo.getBlob());
            Map<String, Serializable> parameters = new HashMap<String, Serializable>();
            parameters.put("videoInfo", originalVideo.getVideoInfo());
            parameters.put("videoExportInfo", vei);
            ConversionService conversionService = Framework.getLocalService(ConversionService.class);
            BlobHolder result = conversionService.convert(
                    "genericVideoConverter", blobHolder, parameters);
            VideoInfo videoInfo = VideoHelper.getVideoInfo(result.getBlob());
            return TranscodedVideo.fromBlobAndInfo(vei.getTitle(),
                    result.getBlob(), videoInfo);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    protected void saveNewTranscodedVideo(DocumentModel doc,
            TranscodedVideo transcodedVideo) throws ClientException {
        if (!doc.hasFacet(VIDEO_EXPORT_FACET)) {
            doc.addFacet(VIDEO_EXPORT_FACET);
        }
        List<Map<String, Serializable>> transcodedVideos = (List<Map<String, Serializable>>) doc.getPropertyValue(EXPORTED_VIDEOS_PROPERTY);
        if (transcodedVideos == null) {
            transcodedVideos = new ArrayList<>();
        }
        transcodedVideos.add(transcodedVideo.toMap());
        doc.setPropertyValue(EXPORTED_VIDEOS_PROPERTY,
                (Serializable) transcodedVideos);
        if (doc.isVersion()) {
            doc.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);
        }
        session.saveDocument(doc);
    }

}
