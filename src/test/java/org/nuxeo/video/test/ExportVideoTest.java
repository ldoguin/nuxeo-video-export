/**
 *
 */

package org.nuxeo.video.test;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 *
 * @author ldoguin
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy({ "nuxeo-video-export" })
public class ExportVideoTest {

    @Inject
    CoreSession coreSession;

    @Inject
    DirectoryService directoryService;

    @Test
    public void testVideoDirectories() throws Exception {
        Session videoProfileSession = directoryService.open("videoProfile");
        assertNotNull(videoProfileSession);
        Session videoTargetSession = directoryService.open("videoTarget");
        assertNotNull(videoTargetSession);
    }

}