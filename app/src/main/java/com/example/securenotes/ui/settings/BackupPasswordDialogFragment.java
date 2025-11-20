package com.example.securenotes.ui.settings;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.example.securenotes.R;


public class BackupPasswordDialogFragment extends DialogFragment {

    public interface BackupPasswordListener {
        void onBackupPasswordSet(String password);
    }

    private BackupPasswordListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (getTargetFragment() instanceof BackupPasswordListener) {
            listener = (BackupPasswordListener) getTargetFragment();
        } else {
            throw new RuntimeException("Parent must implement BackupPasswordListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_backup_password, null);
        final EditText etPass = view.findViewById(R.id.etPassword);
        final EditText etConfirm = view.findViewById(R.id.etConfirmPassword);
        final TextView tvError = view.findViewById(R.id.tvErrorMsg);

        builder.setView(view)
                .setTitle(R.string.create_backup)
                .setPositiveButton(R.string.start, null)
                .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            button.setOnClickListener(v -> {
                tvError.setVisibility(View.GONE);
                String password = etPass.getText().toString();
                String confirm = etConfirm.getText().toString();

                if (!password.equals(confirm)) {
                    showError(tvError, getString(R.string.passwords_not_equal));
                    return;
                }
                if (isValidPassword(password)) {
                    listener.onBackupPasswordSet(password);
                    dialog.dismiss();
                } else {
                    showError(tvError, getString(R.string.weak_password));
                }
            });
        });
        return dialog;
    }

    private void showError(TextView tv, String message) {
        tv.setText(message);
        tv.setVisibility(View.VISIBLE);
    }

    private boolean isValidPassword(String password) {
        if (password.length() < 12) return false;

        // almeno una Maiuscola
        if (!password.matches(".*[A-Z].*")) return false;
        // almeno un Numero
        if (!password.matches(".*[0-9].*")) return false;
        // almeno un Carattere Speciale
        if (!password.matches(".*[@#$%^&+=!.].*")) return false;

        return true;
    }
}