package com.cadetia.simplicadet.ui.home;

import static android.app.Activity.RESULT_OK;
import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.cadetia.simplicadet.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class HomeFragment2 extends Fragment {

    private ActivityResultLauncher<Intent> cameraLauncher;
    private Uri photoUri;
    private ImageView medImageView;

    private TextView extractedTextView;

    // Use the new generativelanguage endpoint
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=AIzaSyBdL9iHFUtltdK29ijVwcCt3A0a7aYF-aI";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home2, container, false);

        extractedTextView = view.findViewById(R.id.extractedTextView);
        medImageView = view.findViewById(R.id.medImageView);

        // Initialize camera launcher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        handleImage(photoUri);
                    }
                }
        );

        FloatingActionButton fab = requireActivity().findViewById(R.id.fabMain);
        fab.setOnClickListener(v -> dispatchCamera());

        return view;
    }

    public void dispatchCamera() {
        File file = new File(requireContext().getExternalFilesDir(null),
                "capture_" + System.currentTimeMillis() + ".jpg");
        photoUri = FileProvider.getUriForFile(requireContext(),
                requireContext().getPackageName() + ".provider", file);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        cameraLauncher.launch(intent);
    }

    private void handleImage(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                    requireContext().getContentResolver(), uri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
            String b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
            sendToGemini(b64);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load image", e);
        }
    }

    private void sendToGemini(String base64Image) {
        JsonObject imagePart = new JsonObject();
        imagePart.addProperty("mime_type", "image/jpeg");
        imagePart.addProperty("data", base64Image);

        JsonObject inlineData = new JsonObject();
        inlineData.add("inline_data", imagePart);

        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", "Din această imagine cu o rețetă medicală, extrage doar următoarele: diagnosticul pacientului, numele fiecărui medicament prescris și oferă un link ilustrativ pentru o imagine a cutiei fiecărui medicament (poate fi un link reprezentativ de pe Google Images). Răspunsul să fie clar structurat, doar în text.");

        JsonArray parts = new JsonArray();
        parts.add(textPart);
        parts.add(inlineData);

        JsonObject content = new JsonObject();
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject body = new JsonObject();
        body.add("contents", contents);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(GEMINI_URL)
                .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Gemini generateContent failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Gemini error code: " + response.code());
                    return;
                }
                String json = response.body().string();
                parseAndLog(json);
            }
        });
    }


    private void parseAndLog(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (root.has("candidates")) {
            JsonArray candidates = root.getAsJsonArray("candidates");
            for (int i = 0; i < candidates.size(); i++) {
                JsonObject candidate = candidates.get(i).getAsJsonObject();
                if (candidate.has("content")) {
                    JsonObject content = candidate.getAsJsonObject("content");
                    if (content.has("parts")) {
                        JsonArray parts = content.getAsJsonArray("parts");
                        if (parts.size() > 0) {
                            JsonObject part = parts.get(0).getAsJsonObject();
                            if (part.has("text")) {
                                String text = part.get("text").getAsString();
                                Log.d(TAG, "Candidate[" + i + "]: " + text);
                                requireActivity().runOnUiThread(() -> {
                                    extractedTextView.setText(text);

                                    // Cautăm un link în text (link către poză medicament)
                                    Pattern pattern = Pattern.compile("https?://\\S+\\.(png|jpg|jpeg)");
                                    Matcher matcher = pattern.matcher(text);
                                    if (matcher.find()) {
                                        String imageUrl = matcher.group();
                                        Glide.with(requireContext())
                                                .load(imageUrl)
                                                .into(medImageView);
                                    } else {
                                        medImageView.setImageDrawable(null);
                                    }
                                });

                            }
                        }
                    }
                }
            }
        } else {
            Log.w(TAG, "No candidates in Gemini response");
        }
    }


}
