/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 *  Authors: Adrien Béraud <adrien.beraud@savoirfairelinux.com>
 *           Rayan Osseiran <rayan.osseiran@savoirfairelinux.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.seme.tv.account;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ch.seme.R;
import ch.seme.application.FizzleApplication;
import ch.seme.databinding.TvFragShareBinding;
import ch.seme.model.Account;
import ch.seme.mvp.BaseSupportFragment;
import ch.seme.mvp.GenericView;
import ch.seme.services.VCardServiceImpl;
import ch.seme.share.SharePresenter;
import ch.seme.share.ShareViewModel;
import ch.seme.utils.Log;
import ch.seme.utils.QRCodeUtils;
import ch.seme.views.AvatarDrawable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class TVShareFragment extends BaseSupportFragment<SharePresenter> implements GenericView<ShareViewModel> {

    private TvFragShareBinding binding;
    private final CompositeDisposable disposable = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = TvFragShareBinding.inflate(inflater, container, false);
        ((FizzleApplication) getActivity().getApplication()).getInjectionComponent().inject(this);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        disposable.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposable.dispose();
    }

    @Override
    public void showViewModel(final ShareViewModel viewModel) {
        if (binding == null)
            return;

        final QRCodeUtils.QRCodeData qrCodeData = viewModel.getAccountQRCodeData(0x00000000, 0xFFFFFFFF);
        getUserAvatar(viewModel.getAccount());

        if (qrCodeData == null) {
            binding.qrImage.setVisibility(View.INVISIBLE);
        } else {
            int pad = 56;
            Bitmap bitmap = Bitmap.createBitmap(qrCodeData.getWidth() + (2 * pad), qrCodeData.getHeight() + (2 * pad), Bitmap.Config.ARGB_8888);
            bitmap.setPixels(qrCodeData.getData(), 0, qrCodeData.getWidth(), pad, pad, qrCodeData.getWidth(), qrCodeData.getHeight());
            binding.qrImage.setImageBitmap(bitmap);
            binding.shareQrInstruction.setText(R.string.share_message);
            binding.qrImage.setVisibility(View.VISIBLE);
        }
    }

    private void getUserAvatar(Account account) {
        disposable.add(VCardServiceImpl
                .loadProfile(account)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(profile -> {
                    if (binding != null) {
                        binding.shareUri.setVisibility(View.VISIBLE);
                        binding.shareUri.setText(account.getDisplayUsername());
                    }
                })
                .flatMap(p -> AvatarDrawable.load(requireContext(), account))
                .subscribe(a -> {
                    if (binding != null) {
                        binding.qrUserPhoto.setVisibility(View.VISIBLE);
                        binding.qrUserPhoto.setImageDrawable(a);
                    }
                }, e-> Log.e(TVShareFragment.class.getSimpleName(), e.getMessage())));
    }
}
