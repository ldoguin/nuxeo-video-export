package org.nuxeo.video;

import static org.nuxeo.ecm.platform.video.convert.Constants.INPUT_FILE_PATH_PARAMETER;
import static org.nuxeo.ecm.platform.video.convert.Constants.OUTPUT_FILE_NAME_PARAMETER;
import static org.nuxeo.ecm.platform.video.convert.Constants.OUTPUT_FILE_PATH_PARAMETER;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolderWithProperties;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.convert.plugins.CommandLineBasedConverter;
import org.nuxeo.ecm.platform.video.VideoInfo;
import org.nuxeo.runtime.api.Framework;

public class GenericVideoConverter extends CommandLineBasedConverter {

    @Override
    protected Map<String, Blob> getCmdBlobParameters(BlobHolder blobHolder,
            Map<String, Serializable> stringSerializableMap)
            throws ConversionException {
        Map<String, Blob> cmdBlobParams = new HashMap<String, Blob>();
        try {
            cmdBlobParams.put(INPUT_FILE_PATH_PARAMETER, blobHolder.getBlob());
        } catch (ClientException e) {
            throw new ConversionException("Unable to get Blob for holder", e);
        }
        return cmdBlobParams;
    }

    @Override
    protected Map<String, String> getCmdStringParameters(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {
        Map<String, String> cmdStringParams = new HashMap<String, String>();
        VideoExportInfo vie = (VideoExportInfo) parameters.get("videoExportInfo");
        cmdStringParams.put("acodec", vie.acodec);
        cmdStringParams.put("vcodec", vie.vcodec);
        cmdStringParams.put("mimetype", vie.mimeType);

        String baseDir = getTmpDirectory(parameters);
        Path tmpPath = new Path(baseDir).append("convertTo" + vie.extension
                + "_" + UUID.randomUUID());

        File outDir = new File(tmpPath.toString());
        boolean dirCreated = outDir.mkdir();
        if (!dirCreated) {
            throw new ConversionException(
                    "Unable to create tmp dir for transformer output: "
                            + outDir);
        }

        try {
            File outFile = File.createTempFile("videoConversion", "."
                    + vie.extension, outDir);
            // delete the file as we need only the path for ffmpeg
            outFile.delete();
            Framework.trackFile(outFile, this);
            cmdStringParams.put(OUTPUT_FILE_PATH_PARAMETER,
                    outFile.getAbsolutePath());
            String baseName = FilenameUtils.getBaseName(blobHolder.getBlob().getFilename());
            cmdStringParams.put(OUTPUT_FILE_NAME_PARAMETER, baseName + "."
                    + vie.extension);

            VideoInfo videoInfo = (VideoInfo) parameters.get("videoInfo");
            if (videoInfo == null) {
                return cmdStringParams;
            }

            long width = videoInfo.getWidth();
            long height = videoInfo.getHeight();
            long newHeight = vie.height;

            long newWidth = width * newHeight / height;
            if (newWidth % 2 != 0) {
                newWidth += 1;
            }

            cmdStringParams.put("width", String.valueOf(newWidth));
            cmdStringParams.put("height", String.valueOf(newHeight));
            return cmdStringParams;
        } catch (Exception e) {
            throw new ConversionException("Unable to get Blob for holder", e);
        }
    }

    @Override
    protected BlobHolder buildResult(List<String> cmdOutput,
            CmdParameters cmdParameters) throws ConversionException {
        String outputPath = cmdParameters.getParameters().get(
                OUTPUT_FILE_PATH_PARAMETER);
        String mimeType = cmdParameters.getParameters().get("mimetype");
        File outputFile = new File(outputPath);
        List<Blob> blobs = new ArrayList<Blob>();
        String outFileName = cmdParameters.getParameters().get(
                OUTPUT_FILE_NAME_PARAMETER);
        if (outFileName == null) {
            outFileName = outputFile.getName();
        } else {
            outFileName = unquoteValue(outFileName);
        }

        Blob blob = new FileBlob(outputFile);
        blob.setFilename(outFileName);
        blob.setMimeType(mimeType);
        blobs.add(blob);

        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put("cmdOutput", (Serializable) cmdOutput);
        return new SimpleBlobHolderWithProperties(blobs, properties);
    }

    /**
     * @since 5.6
     */
    protected String unquoteValue(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

}
