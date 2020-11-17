package io.openschema.mma;

import android.content.Context;
import android.util.Log;

import org.spongycastle.operator.OperatorCreationException;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import io.openschema.mma.bootstrap.BootStrapManager;
import io.openschema.mma.id.Identity;
import io.openschema.mma.networking.BackendApi;
import io.openschema.mma.networking.RetrofitService;
import io.openschema.mma.register.RegistrationManager;

public class MobileMetricsAgent {

    private static final String TAG = "MobileMetricsAgent";

    private String mControllerAddress;
    private String mBoostStrapperAddress;
    private String mMetricsAuthorityHeader;
    private int mControllerPort;

    private String mBackendBaseURL;
    private int mCertificateResId;

    private Context mAppContext;
    private RegistrationManager mRegistrationManager;
    private BootStrapManager mBootStrapManager;

    private MobileMetricsAgent(Builder mmaBuilder) {
        mBoostStrapperAddress = mmaBuilder.mBoostStrapperAddress;
        mControllerAddress = mmaBuilder.mControllerAddress;
        mMetricsAuthorityHeader = mmaBuilder.mMetricsAuthorityHeader;
        mControllerPort = mmaBuilder.mControllerPort;

        mBackendBaseURL = mmaBuilder.mBackendBaseURL;
        mCertificateResId = mmaBuilder.mCertificateResId;

        mAppContext = mmaBuilder.mAppContext;
    }

    //Will register the UE & run the bootstrap process for GRPC
    public void init() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException {
        Log.d(TAG, "MMA: Initializing MMA...");
        Identity identity = new Identity(mAppContext);

        RetrofitService mRetrofitService = RetrofitService.getService(mAppContext);
        mRetrofitService.initApi(mAppContext, mBackendBaseURL, mCertificateResId);
        BackendApi mBackendApi = mRetrofitService.getApi();

        mRegistrationManager = new RegistrationManager(mAppContext, identity, mBackendApi);
        mBootStrapManager = new BootStrapManager(mAppContext, identity);


        //TODO: spawn a background thread instead? Bootstrapping process is currently blocking the main thread
        mRegistrationManager.setOnRegisterListener(() -> {
            try {
                mBootStrapManager.bootstrapNow(mBoostStrapperAddress, mControllerPort);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        mRegistrationManager.register();
    }

    public String getControllerAddress() {
        return mControllerAddress;
    }

    public String getBoostStrapperAddress() {
        return mBoostStrapperAddress;
    }

    public String getMetricsAuthorityHeader() {
        return mMetricsAuthorityHeader;
    }

    public int getControllerPort() {
        return mControllerPort;
    }


    public static class Builder {
        private String mControllerAddress;
        private String mBoostStrapperAddress;
        private String mMetricsAuthorityHeader;
        private int mControllerPort;

        private String mBackendBaseURL;
        private int mCertificateResId;

        private Context mAppContext;


        public Builder setControllerAddress(String address) {
            mControllerAddress = address;
            return this;
        }

        public Builder setBootStrapperAddress(String address) {
            mBoostStrapperAddress = address;
            return this;
        }

        public Builder setAuthorityHeader(String address) {
            mMetricsAuthorityHeader = address;
            return this;
        }

        public Builder setControllerPort(int port) {
            mControllerPort = port;
            return this;
        }

        public Builder setBackendBaseURL(String baseURL) {
            mBackendBaseURL = baseURL;
            return this;
        }

        public Builder setCertificateResId(int certificateResId) {
            mCertificateResId = certificateResId;
            return this;
        }

        public Builder setAppContext(Context context) {
            mAppContext = context;
            return this;
        }

        public MobileMetricsAgent build() {
            return new MobileMetricsAgent(this);
        }
    }
}
