package com.example.nhom8_makafe.ui.login;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.nhom8_makafe.data.api.ApiCallback;
import com.example.nhom8_makafe.data.api.ApiRepository;
import com.example.nhom8_makafe.databinding.FragmentLoginBinding;
import com.example.nhom8_makafe.model.User;
import com.example.nhom8_makafe.util.ImageLoader;

public class LoginFragment extends Fragment {
    private FragmentLoginBinding binding;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ApiRepository apiRepository = ApiRepository.getInstance();

    public static LoginFragment newInstance() {
        return new LoginFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindLoginAvatar(null);
        loadLoginBrandingAvatar();
        binding.buttonLogin.setOnClickListener(v -> submitLogin());
    }

    private void loadLoginBrandingAvatar() {
        apiRepository.fetchLoginBranding(new ApiCallback<String>() {
            @Override
            public void onSuccess(String data) {
                if (!isAdded() || binding == null) {
                    return;
                }
                bindLoginAvatar(data);
            }

            @Override
            public void onError(String message) {
                if (!isAdded() || binding == null) {
                    return;
                }
                bindLoginAvatar(null);
            }
        });
    }

    private void bindLoginAvatar(@Nullable String imageUrl) {
        if (binding == null) {
            return;
        }
        boolean hasImage = !TextUtils.isEmpty(imageUrl);
        binding.imageLoginAvatar.setVisibility(hasImage ? View.VISIBLE : View.GONE);
        binding.textLoginAvatarFallback.setVisibility(View.VISIBLE);
        if (!hasImage) {
            binding.imageLoginAvatar.setImageDrawable(null);
            return;
        }
        ImageLoader.load(binding.imageLoginAvatar, imageUrl, android.R.color.transparent);
    }

    private void submitLogin() {
        String username = String.valueOf(binding.editUsername.getText()).trim();
        String password = String.valueOf(binding.editPassword.getText()).trim();

        binding.inputUsername.setError(null);
        binding.inputPassword.setError(null);
        binding.textError.setVisibility(View.GONE);

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            binding.textError.setText("Vui lòng nhập đầy đủ thông tin");
            binding.textError.setVisibility(View.VISIBLE);
            if (TextUtils.isEmpty(username)) {
                binding.inputUsername.setError("Bắt buộc");
            }
            if (TextUtils.isEmpty(password)) {
                binding.inputPassword.setError("Bắt buộc");
            }
            return;
        }

        setLoading(true);
        handler.postDelayed(() -> {
            if (!isAdded() || binding == null) {
                return;
            }
            apiRepository.login(username, password, new ApiCallback<User>() {
                @Override
                public void onSuccess(User data) {
                    if (!isAdded() || binding == null) {
                        return;
                    }
                    setLoading(false);
                }

                @Override
                public void onError(String message) {
                    if (!isAdded() || binding == null) {
                        return;
                    }
                    setLoading(false);
                    binding.textError.setText(message == null || message.trim().isEmpty()
                            ? "Không thể đăng nhập. Vui lòng thử lại."
                            : message);
                    binding.textError.setVisibility(View.VISIBLE);
                }
            });
        }, 300);
    }

    private void setLoading(boolean loading) {
        binding.progressLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.buttonLogin.setEnabled(!loading);
        binding.buttonLogin.setText(loading ? "Đang đăng nhập..." : "Đăng nhập");
        binding.editUsername.setEnabled(!loading);
        binding.editPassword.setEnabled(!loading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        binding = null;
    }
}
