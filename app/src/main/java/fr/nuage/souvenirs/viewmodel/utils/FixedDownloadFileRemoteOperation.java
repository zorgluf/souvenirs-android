package fr.nuage.souvenirs.viewmodel.utils;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.OperationCancelledException;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class FixedDownloadFileRemoteOperation extends RemoteOperation {

    private static final String TAG = FixedDownloadFileRemoteOperation.class.getSimpleName();

    private Set<OnDatatransferProgressListener> mDataTransferListeners = new HashSet<OnDatatransferProgressListener>();
    private final AtomicBoolean mCancellationRequested = new AtomicBoolean(false);
    private long mModificationTimestamp = 0;
    private String mEtag = "";
    private GetMethod mGet;

    private String mRemotePath;
    private String mLocalFolderPath;

    public FixedDownloadFileRemoteOperation(String remotePath, String localFolderPath) {
        mRemotePath = remotePath;
        mLocalFolderPath = localFolderPath;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;

        /// download will be performed to a temporal file, then moved to the final location
        File tmpFile = new File(getTmpPath());

        /// perform the download
        try {
            tmpFile.getParentFile().mkdirs();
            int status = downloadFile(client, tmpFile);
            result = new RemoteOperationResult(isSuccess(status), mGet);
            Log_OC.i(TAG, "Download of " + mRemotePath + " to " + getTmpPath() + ": " +
                    result.getLogMessage());

        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Download of " + mRemotePath + " to " + getTmpPath() + ": " +
                    result.getLogMessage(), e);
        }

        return result;
    }


    protected int downloadFile(OwnCloudClient client, File targetFile) throws
            IOException {
        int status = -1;
        boolean savedFile = false;
        mGet = new GetMethod(client.getWebdavUri() + WebdavUtils.encodePath(mRemotePath));
        Iterator<OnDatatransferProgressListener> it = null;

        FileOutputStream fos = null;
        try {
            status = client.executeMethod(mGet);
            if (isSuccess(status)) {
                targetFile.createNewFile();
                BufferedInputStream bis = new BufferedInputStream(mGet.getResponseBodyAsStream());
                fos = new FileOutputStream(targetFile);
                long transferred = 0;

                Header contentLength = mGet.getResponseHeader("Content-Length");
                long totalToTransfer = (contentLength != null &&
                        contentLength.getValue().length() > 0) ?
                        Long.parseLong(contentLength.getValue()) : 0;

                byte[] bytes = new byte[4096];
                int readResult = 0;
                while ((readResult = bis.read(bytes)) != -1) {
                    synchronized (mCancellationRequested) {
                        if (mCancellationRequested.get()) {
                            mGet.abort();
                            throw new OperationCancelledException();
                        }
                    }
                    fos.write(bytes, 0, readResult);
                    transferred += readResult;
                    synchronized (mDataTransferListeners) {
                        it = mDataTransferListeners.iterator();
                        while (it.hasNext()) {
                            it.next().onTransferProgress(readResult, transferred, totalToTransfer,
                                    targetFile.getName());
                        }
                    }
                }
                // Check if the file is completed
                // if transfer-encoding: chunked we cannot check if the file is complete
                Header transferEncodingHeader = mGet.getResponseHeader("Transfer-Encoding");
                boolean transferEncoding = false;

                if (transferEncodingHeader != null) {
                    transferEncoding = transferEncodingHeader.getValue().equals("chunked");
                }

                if (transferred == totalToTransfer || transferEncoding) {
                    savedFile = true;
                    Header modificationTime = mGet.getResponseHeader("Last-Modified");
                    if (modificationTime == null) {
                        modificationTime = mGet.getResponseHeader("last-modified");
                    }
                    if (modificationTime != null) {
                        Date d = WebdavUtils.parseResponseDate(modificationTime.getValue());
                        mModificationTimestamp = (d != null) ? d.getTime() : 0;
                    } else {
                        Log_OC.e(TAG, "Could not read modification time from response downloading " + mRemotePath);
                    }

                    mEtag = WebdavUtils.getEtagFromResponse(mGet);
                    if (mEtag.length() == 0) {
                        Log_OC.e(TAG, "Could not read eTag from response downloading " + mRemotePath);
                    }

                } else {
                    client.exhaustResponse(mGet.getResponseBodyAsStream());
                }

            } else {
                client.exhaustResponse(mGet.getResponseBodyAsStream());
            }

        } catch (Exception e) {
            Log_OC.e(TAG, e.getMessage());
        } finally {
            if (fos != null) fos.close();
            if (!savedFile && targetFile.exists()) {
                targetFile.delete();
            }
            mGet.releaseConnection();    // let the connection available for other methods
        }
        return status;
    }

    private boolean isSuccess(int status) {
        return (status == HttpStatus.SC_OK);
    }

    private String getTmpPath() {
        return mLocalFolderPath + File.separator + new File(mRemotePath).getName();
    }

    public void addDatatransferProgressListener(OnDatatransferProgressListener listener) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.add(listener);
        }
    }

    public void removeDatatransferProgressListener(OnDatatransferProgressListener listener) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.remove(listener);
        }
    }

    public void cancel() {
        mCancellationRequested.set(true);   // atomic set; there is no need of synchronizing it
    }

    public long getModificationTimestamp() {
        return mModificationTimestamp;
    }

    public String getEtag() {
        return mEtag;
    }   private String localFolderFile;

}
