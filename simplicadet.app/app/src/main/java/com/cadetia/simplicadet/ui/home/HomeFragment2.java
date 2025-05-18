package com.cadetia.simplicadet.ui.home;

import static android.app.Activity.RESULT_OK;
import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.cadetia.simplicadet.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HomeFragment2 extends Fragment {

    private ActivityResultLauncher<Intent> cameraLauncher;
    private Uri photoUri;
    private boolean isLoadingDismissed = false;

    private RecyclerView medRecyclerView;
    private MedicationAdapter medicationAdapter;
    private List<Medication> medicationList = new ArrayList<>();
    private TextView extractedTextView;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=AIzaSyBdL9iHFUtltdK29ijVwcCt3A0a7aYF-aI";
    private static final String GOOGLE_CSE_URL = "https://www.googleapis.com/customsearch/v1";
    private static final String GOOGLE_API_KEY = "AIzaSyC1k9LDYklFJE9BgrO6FvRkSCWm_BzaGsk";
    private static final String CSE_ID = "763bb434673cf4931";
    private View loadingLayout;
    private View contentView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home2, container, false);

        loadingLayout = view.findViewById(R.id.layout_loading);
        contentView = view.findViewById(R.id.contentLayout2);

        medRecyclerView = view.findViewById(R.id.medRecyclerView);
        medicationAdapter = new MedicationAdapter(medicationList);
        medRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        medRecyclerView.setAdapter(medicationAdapter);

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
        textPart.addProperty("text", "Din această imagine cu o rețetă medicală, extrage numele fiecărui medicament prescris, fara dozaj sau gramaj. Răspunsul să fie în format JSON structurat astfel: [{\"nume\": \"NumeMedicament\"}]");

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
        showLoading(true);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (root.has("candidates")) {
            JsonArray candidates = root.getAsJsonArray("candidates");
            for (JsonElement candElem : candidates) {
                JsonObject candidate = candElem.getAsJsonObject();
                if (candidate.has("content")) {
                    JsonObject content = candidate.getAsJsonObject("content");
                    if (content.has("parts")) {
                        JsonArray parts = content.getAsJsonArray("parts");
                        if (parts.size() > 0) {
                            String text = parts.get(0).getAsJsonObject().get("text").getAsString();
                            Log.e("AI", text);
                            try {
                                medicationList.clear();

                                String trimmed = text.trim()
                                        .replaceAll("(?m)^```.*|```\\s*$", "")
                                        .trim();

                                int start = trimmed.indexOf('[');
                                int end   = trimmed.lastIndexOf(']');
                                if (start != -1 && end != -1 && end > start) {
                                    String arrayStr = trimmed.substring(start, end + 1);
                                    JSONArray jsonArray = new JSONArray(arrayStr);

                                    for (int j = 0; j < jsonArray.length(); j++) {
                                        JSONObject item = jsonArray.getJSONObject(j);
                                        String nume = item.optString("nume", "Nedefinit");
                                        Medication med = new Medication(nume, "https://placehold.co/150");
                                        medicationList.add(med);
                                        fetchGoogleImageUrl(med);

                                    }
                                } else {
                                    for (String line : trimmed.split("\\r?\\n")) {
                                        line = line.trim();
                                        if (!line.isEmpty()) {
                                            Medication med = new Medication(line, "https://placehold.co/150");
                                            medicationList.add(med);
                                            fetchGoogleImageUrl(med);
                                        }
                                    }
                                }

                                requireActivity().runOnUiThread(() -> {
                                    showLoading(false);
                                    medicationAdapter.notifyDataSetChanged();
                                });

                            } catch (JSONException e) {
                                Log.e(TAG, "JSON parsing failed", e);
                                requireActivity().runOnUiThread(() -> {
                                    showLoading(false);
                                });
                            }
                        }
                    }
                }
            }
        } else {
            requireActivity().runOnUiThread(() -> {
                showLoading(false);
                extractedTextView.setText("Fără candidați în răspuns");
            });
        }
    }


    private void showLoading(boolean show) {
        if (loadingLayout != null && contentView != null) {
            if (show) {
                isLoadingDismissed = false;
                loadingLayout.setVisibility(View.VISIBLE);
                contentView.setVisibility(View.GONE);
            } else if (!isLoadingDismissed) {
                isLoadingDismissed = true;

                Context context = getContext();
                if (context != null) {
                    loadingLayout.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_out));
                    new Handler().postDelayed(() -> {
                        if (loadingLayout != null && isAdded()) {
                            loadingLayout.setVisibility(View.GONE);
                            contentView.setVisibility(View.VISIBLE);
                            contentView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in));
                        }
                    }, 250);
                } else {
                    Log.w(TAG, "Context is null, skipping animations");
                }
            }
        }
    }

    private void fetchGoogleImageUrl(Medication medication) {
        String query = Uri.encode(medication.getName());
        String url = GOOGLE_CSE_URL + "?q=" + query + "&key=" + GOOGLE_API_KEY + "&cx=" + CSE_ID + "&searchType=image";

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Google CSE failed for " + medication.getName(), e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Google CSE error: " + response.code());
                    return;
                }

                try {
                    String jsonResponse = response.body().string();
                    JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();
                    JsonArray items = json.getAsJsonArray("items");

                    if (items != null && items.size() > 0) {
                        String imageUrl = items.get(0).getAsJsonObject().get("link").getAsString();
                        medication.setImageUrl(imageUrl);

                        requireActivity().runOnUiThread(() -> {
                            int position = medicationList.indexOf(medication);
                            if (position != -1) {
                                medicationAdapter.notifyItemChanged(position);
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing Google CSE response", e);
                }
            }
        });
    }

    public static class Medication {
        private String name;
        private String imageUrl;

        public Medication(String name, String imageUrl) {
            this.name = name;
            this.imageUrl = imageUrl;
        }

        public String getName() { return name; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    }

    public class MedicationAdapter extends RecyclerView.Adapter<MedicationAdapter.MedicationViewHolder> {

        private List<Medication> medications;

        public MedicationAdapter(List<Medication> medications) {
            this.medications = medications;
        }

        @NonNull
        @Override
        public MedicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_quizz, parent, false);
            return new MedicationViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MedicationViewHolder holder, int position) {
            Medication medication = medications.get(position);
            holder.bind(medication);
        }

        @Override
        public int getItemCount() {
            return medications.size();
        }

        class MedicationViewHolder extends RecyclerView.ViewHolder {

            ImageView medicationImage;
            TextView medicationName;

            public MedicationViewHolder(@NonNull View itemView) {
                super(itemView);
                medicationImage = itemView.findViewById(R.id.imageQuizz);
                medicationName = itemView.findViewById(R.id.quizzTitle);
            }

            public void bind(Medication medication) {
                medicationName.setText(medication.getName());
                Glide.with(itemView.getContext())
                        .load(medication.getImageUrl())
                        .placeholder(R.drawable.background_nothing_rounded)
                        .error(R.drawable.background_nothing_rounded)
                        .into(medicationImage);
            }
        }
    }
}