package com.aa65535.nekoatsumearchivemodifier;

import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class MainActivity extends AppCompatActivity implements OnClickListener {
    private static final int OFFSET_FISH = 0x14;
    private static final int OFFSET_GOLD_FISH = 0x18;

    private EditText silverFishInput;
    private EditText goldFishInput;
    private Button silverFishButton;
    private Button goldFishButton;

    private File archive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        archive = new File(getCacheDir().getParentFile().getParentFile(),
                "jp.co.hit_point.nekoatsume/files/savedata_system.sav");
        if (upgradeRootPermission(archive)) {
            initView();
        } else {
            showToast(R.string.archive_permission_denied);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initData();
    }

    private void initView() {
        silverFishInput = findViewById(R.id.et_silver_fish);
        goldFishInput = findViewById(R.id.et_gold_fish);
        silverFishButton = findViewById(R.id.save_silver_fish);
        goldFishButton = findViewById(R.id.save_gold_fish);
        silverFishButton.setOnClickListener(this);
        goldFishButton.setOnClickListener(this);
        silverFishInput.addTextChangedListener(new MyTextWatcher(silverFishButton));
        goldFishInput.addTextChangedListener(new MyTextWatcher(goldFishButton));
    }

    private void initData() {
        if (archive.canWrite()) {
            String fishData = getString(R.string.number, readInt(archive, OFFSET_FISH));
            String goldFishData = getString(R.string.number, readInt(archive, OFFSET_GOLD_FISH));
            silverFishButton.setTag(fishData);
            goldFishButton.setTag(goldFishData);
            silverFishInput.setText(fishData);
            goldFishInput.setText(goldFishData);
        } else {
            showToast(R.string.archive_permission_denied);
        }
    }

    @Override
    public void onClick(View v) {
        try {
            String s = null;
            boolean ret = false;
            switch (v.getId()) {
                case R.id.save_silver_fish:
                    s = silverFishInput.getText().toString();
                    ret = writeInt(archive, OFFSET_FISH, Integer.parseInt(s));
                    break;
                case R.id.save_gold_fish:
                    s = goldFishInput.getText().toString();
                    ret = writeInt(archive, OFFSET_GOLD_FISH, Integer.parseInt(s));
                    break;
            }
            v.setTag(s);
            v.setEnabled(!ret);
            showToast(ret ? R.string.success_msg : R.string.failure_msg);
        } catch (NumberFormatException e) {
            showToast(R.string.number_err_msg);
        }
    }

    private void showToast(@StringRes int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    private static void destroyQuietly(Process process) {
        if (process != null) {
            try {
                process.destroy();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    private static int readInt(File archive, int offset) {
        RandomAccessFile r = null;
        try {
            r = new RandomAccessFile(archive, "r");
            r.seek(offset);
            return r.readInt();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(r);
        }
        return -1;
    }

    private static boolean writeInt(File archive, int offset, int value) {
        RandomAccessFile r = null;
        try {
            r = new RandomAccessFile(archive, "rwd");
            r.seek(offset);
            r.writeInt(value);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(r);
        }
        return false;
    }

    public static boolean upgradeRootPermission(File archive) {
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("chmod 666 " + archive.getAbsolutePath() + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (Exception e) {
            return false;
        } finally {
            try {
                closeQuietly(os);
                destroyQuietly(process);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private static class MyTextWatcher implements TextWatcher {
        private Button button;

        MyTextWatcher(Button button) {
            this.button = button;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            button.setEnabled(!s.toString().equals(button.getTag()));
        }
    }
}
