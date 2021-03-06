package nl.lightbase;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;
import android.net.Uri;

import com.facebook.infer.annotation.Assertions;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.google.vr.sdk.widgets.common.VrWidgetView.DisplayMode;
import com.google.vr.sdk.widgets.pano.VrPanoramaEventListener;
import com.google.vr.sdk.widgets.pano.VrPanoramaView;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class PanoramaViewManager extends SimpleViewManager<VrPanoramaView> {
    private static final String REACT_CLASS = "PanoramaView";

    private ReactApplicationContext _context;
    private VrPanoramaView vrPanoramaView;

    private VrPanoramaView.Options _options = new VrPanoramaView.Options();
    private Map<String, Bitmap> imageCache = new HashMap<>();
    private ImageLoaderTask imageLoaderTask;
    private Integer imageWidth;
    private Integer imageHeight;
    private String imageUrl;
    private String imageData;

    public PanoramaViewManager(ReactApplicationContext context) {
        super();
        _context = context;
    }

    public ReactApplicationContext getContext() {
        return _context;
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public void onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy();
        if(vrPanoramaView != null) {
            Log.i(REACT_CLASS, "Shutting gvrsdk down");
            vrPanoramaView.shutdown();
        }
    }

    @Override
    public VrPanoramaView createViewInstance(ThemedReactContext context) {
        vrPanoramaView = new VrPanoramaView(context.getCurrentActivity());
        vrPanoramaView.setEventListener(new ActivityEventListener());

        vrPanoramaView.setDisplayMode(DisplayMode.EMBEDDED);
        vrPanoramaView.setStereoModeButtonEnabled(false);
        vrPanoramaView.setTransitionViewEnabled(false);
        vrPanoramaView.setInfoButtonEnabled(false);
        vrPanoramaView.setFullscreenButtonEnabled(false);

        return vrPanoramaView;
    }

    @Override
    protected void onAfterUpdateTransaction(VrPanoramaView view) {
        super.onAfterUpdateTransaction(view);

        if (imageLoaderTask != null) {
            imageLoaderTask.cancel(true);
        }


        if (imageUrl != null) {
            try {
                imageLoaderTask = new ImageLoaderTask();
                imageLoaderTask.execute(Pair.create(imageUrl, _options));
            } catch (Exception e) {
                emitEvent("onImageLoadingFailed", null);
            }
        }else if(imageData != null) {
            byte[] imageBytes = Base64.decode(imageData, Base64.DEFAULT);
            Bitmap decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            vrPanoramaView.loadImageFromBitmap(decodedImage, _options);
        } else {
            emitEvent("onImageLoadingFailed", null);
        }
    }

    @Override
    public @Nullable Map getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.of(
                "onImageLoadingFailed",
                MapBuilder.of("registrationName", "onImageLoadingFailed"),
                "onImageLoaded",
                MapBuilder.of("registrationName", "onImageLoaded")
        );
    }


    @ReactProp(name = "imageUrl")
    public void setImageSource(VrPanoramaView view, String value) {
        Log.i(REACT_CLASS, "Image source: " + value);

        if (imageUrl != null && imageUrl.equals(value)) {
            return;
        }

        imageUrl = value;
    }

    @ReactProp(name = "imageData")
    public void setImageSourceData(VrPanoramaView view, String value) {
        Log.i(REACT_CLASS, "Image source: " + value);

        if (imageData != null && imageData.toString().equals(value)) {
            return;
        }

        imageData = value;
    }

    @ReactProp(name = "dimensions")
    public void setDimensions(VrPanoramaView view, ReadableMap dimensions) {
        imageWidth = dimensions.getInt("width");
        imageHeight = dimensions.getInt("height");

        Log.i(REACT_CLASS, "Image dimensions: " + imageWidth + ", " + imageHeight);
    }

    @ReactProp(name = "inputType")
    public void setInputType(VrPanoramaView view, String inputType) {
        switch (inputType) {
            case "mono":
                _options.inputType = _options.TYPE_MONO;
                break;
            case "stereo":
                _options.inputType = _options.TYPE_STEREO_OVER_UNDER;
                break;
            default:
                _options.inputType = _options.TYPE_MONO;
        }
    }

    @ReactProp(name = "enableTouchTracking")
    public void setEnableTouchTracking(VrPanoramaView view, boolean enableTouchTracking) {
        view.setTouchTrackingEnabled(enableTouchTracking);
    }

    class ImageLoaderTask extends AsyncTask<Pair<String, VrPanoramaView.Options>, Void, Boolean> {
        protected Boolean doInBackground(Pair<String, VrPanoramaView.Options>... fileInformation) {

            if(isCancelled()){
                return false;
            }

            String value = fileInformation[0].first;
            VrPanoramaView.Options _options = fileInformation[0].second;

            InputStream istr = null;
            Bitmap image;

            if (!imageCache.containsKey(value)) {
                try {
                    Uri imageUri = Uri.parse(value);
                    String scheme = imageUri.getScheme();

                    if(scheme == null || scheme.equalsIgnoreCase("file")){
                        istr = new FileInputStream(new File(imageUri.getPath()));
                    }
                    else{
                        URL url = new URL(value);
    
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.connect();
    
                        istr = connection.getInputStream();
                    }

                    Assertions.assertCondition(istr != null);

                    imageCache.put(value, decodeSampledBitmap(istr));
                } catch (Exception e) {
                    Log.e(REACT_CLASS, "Could not load file: " + e);
                    if(isCancelled()){
                        return false;
                    }
                    emitEvent("onImageLoadingFailed", null);
                    return false;
                } finally {
                    try {
                        if(istr != null){
                            istr.close();
                        }
                    } catch (IOException e) {
                        Log.e(REACT_CLASS, "Could not close input stream: " + e);

                        emitEvent("onImageLoadingFailed", null);
                    }
                }
            }

            image = imageCache.get(imageUrl);

            vrPanoramaView.loadImageFromBitmap(image, _options);

            return true;
        }

        private Bitmap decodeSampledBitmap(InputStream inputStream) throws IOException {
            final byte[] bytes = getBytesFromInputStream(inputStream);
            BitmapFactory.Options options = new BitmapFactory.Options();

            if (imageWidth != 0 && imageHeight != 0) {
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

                options.inSampleSize = calculateInSampleSize(options, imageWidth, imageHeight);
                options.inJustDecodeBounds = false;
            }

            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        }

        private byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            IOUtils.copy(inputStream, baos);

            return baos.toByteArray();
        }

        private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
            // Raw height and width of image
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {

                final int halfHeight = height / 2;
                final int halfWidth = width / 2;

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                    inSampleSize *= 2;
                }
            }

            return inSampleSize;
        }
    }

    private class ActivityEventListener extends VrPanoramaEventListener {
        @Override
        public void onLoadSuccess() {
            Log.i(REACT_CLASS, "Image loaded.");

            emitEvent("onImageLoaded", null);
        }

        @Override
        public void onLoadError(String errorMessage) {
            Log.e(REACT_CLASS, "Error loading panorama: " + errorMessage);

            emitEvent("onImageLoadingFailed", null);
        }
    }

    private void emitEvent(String name, @Nullable WritableMap event) {
        if (event == null) {
            event = Arguments.createMap();
        }

        getContext().getJSModule(RCTEventEmitter.class).receiveEvent(
                vrPanoramaView.getId(),
                name,
                event
        );
    }
}