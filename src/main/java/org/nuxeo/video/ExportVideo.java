/**
 *
 */

package org.nuxeo.video;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;

/**
 * @author ldoguin
 */
@Operation(id = ExportVideo.ID, category = Constants.CAT_CONVERSION, label = "ExportVideo", description = "Export a Video following given parameters.")
public class ExportVideo {

    public static final String ID = "Conversion.ExportVideo";

    public static final Log log = LogFactory.getLog(ExportVideo.class);

    @Param(name = "target")
    protected String target;

    @Param(name = "profile")
    protected String profile;

    @Param(name = "parameters", required = false)
    protected Properties parameters;

    @Context
    WorkManager workManager;

    @Context
    DirectoryService directoryService;

    @Context
    OperationContext ctx;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) throws ClientException {
        Session videoTargetSession = directoryService.open("videoTarget");
        DocumentModel targetEntry = videoTargetSession.getEntry(target);
        String acodec = (String) targetEntry.getPropertyValue("videoTarget:acodec");
        String vcodec = (String) targetEntry.getPropertyValue("videoTarget:vcodec");
        String extension = (String) targetEntry.getPropertyValue("videoTarget:extension");
        String mimetype = (String) targetEntry.getPropertyValue("videoTarget:mimetype");
        String targetLabel = (String) targetEntry.getPropertyValue("videoTarget:label");
        videoTargetSession.close();

        Session videoProfileSession = directoryService.open("videoProfile");
        DocumentModel profiletEntry = videoProfileSession.getEntry(profile);
        Long height = (Long) profiletEntry.getPropertyValue("videoProfile:height");
        Double fps = (Double) profiletEntry.getPropertyValue("videoProfile:fps");
        String profileLabel = (String) profiletEntry.getPropertyValue("videoProfile:label");
        videoProfileSession.close();
        String workflowId = (String) ctx.get("workflowInstanceId");
        String nodeId = (String) ctx.get("nodeId");
        VideoExportInfo vei = new VideoExportInfo(height, fps, extension,
                mimetype, acodec, vcodec, profileLabel, targetLabel);
        CustomVideoConversionWork work = new CustomVideoConversionWork(workflowId, nodeId,
                doc.getRepositoryName(), doc.getId(), vei, parameters);
        workManager.schedule(work, Scheduling.IF_NOT_RUNNING_OR_SCHEDULED);
        return doc;
    }
}
