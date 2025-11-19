package com.example.securenotes.ui.auth;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.securenotes.R;
import com.example.securenotes.databinding.FragmentEnterPinBinding;
import com.example.securenotes.security.PinManager;

/*
Fragment per inserire il PIN (fallback biometria).
*/
public class EnterPinFragment extends Fragment {

    // Interfaccia callback per notificare AuthActivity
    public interface PinAuthenticationListener {
        void onPinAuthenticated();
    }

    private FragmentEnterPinBinding binding;
    private PinAuthenticationListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Activity deve implementare listener
        if (context instanceof PinAuthenticationListener) {
            listener = (PinAuthenticationListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement PinAuthenticationListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEnterPinBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonUnlock.setOnClickListener(v -> {
            String pin = binding.editTextPin.getText().toString();

            // Controlla se il PIN è corretto usando PinManager
            if (PinManager.isPinCorrect(requireContext(), pin)) {
                // PIN CORRETTO: Notifica l'activity
                listener.onPinAuthenticated();
            } else {
                Toast.makeText(getContext(), R.string.wrong_pin, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}