package com.cadetia.simplicadet.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cadetia.simplicadet.R;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

public class PdfViewerActivity extends AppCompatActivity {

    private PDFView pdfView;
    private View progressBar;
    private String pdfUrl;
    private String pdfTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);

        findViewById(R.id.pdf_back).setOnClickListener(v -> finish());
        pdfView = findViewById(R.id.pdfView);
        progressBar = findViewById(R.id.progressBar);

        if (getIntent().hasExtra("pdfUrl")) {
            pdfUrl = getIntent().getStringExtra("pdfUrl");
            pdfTitle = getIntent().getStringExtra("pdfTitle");

            if (pdfUrl != null && !pdfUrl.isEmpty()) {
                downloadAndShowPdf(pdfUrl);
            } else {
                Toast.makeText(this, "Invalid PDF URL", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "No PDF URL provided", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void downloadAndShowPdf(String url) {
        progressBar.setVisibility(View.VISIBLE);
        String uniqueFilename = pdfTitle.replaceAll("[^a-zA-Z0-9.-]", "_") + ".pdf";
        File targetFile = new File(getExternalFilesDir(null), uniqueFilename);

        if (targetFile.exists()) {
            progressBar.setVisibility(View.GONE);
            loadPdfFromFile(targetFile);
            return;
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(PdfViewerActivity.this, "Failed to download PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(PdfViewerActivity.this, "Failed to download PDF: " + response.code(), Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    response.close();
                    return;
                }

                try {
                    BufferedSink sink = Okio.buffer(Okio.sink(targetFile));
                    sink.writeAll(response.body().source());
                    sink.close();

                    runOnUiThread(() -> {
                        loadPdfFromFile(targetFile);
                    });

                } catch (IOException e) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(PdfViewerActivity.this, "Failed to save PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } finally {
                    response.close();
                }
            }
        });
    }

    private void loadPdfFromFile(File file) {
        pdfView.fromFile(file)
                .defaultPage(0)
                .enableSwipe(true)
                .scrollHandle(new DefaultScrollHandle(PdfViewerActivity.this))
                .spacing(10)
                .onLoad(new OnLoadCompleteListener() {
                    @Override
                    public void loadComplete(int nbPages) {
                        progressBar.setVisibility(View.GONE);
                    }
                })
                .onError(new OnErrorListener() {
                    @Override
                    public void onError(Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(PdfViewerActivity.this, "Error loading PDF: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .load();
    }

}