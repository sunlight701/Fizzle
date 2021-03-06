/*
 *  Copyright (C) 2004-2020 Savoir-faire Linux Inc.
 *
 *  Author: Hadrien De Sousa <hadrien.desousa@savoirfairelinux.com>
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
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package ch.seme.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import java.util.Map;

import ch.seme.model.Account;
import ch.seme.model.CallContact;
import ch.seme.utils.BitmapUtils;
import ch.seme.utils.FileUtils;
import ch.seme.utils.Tuple;
import ch.seme.utils.VCardUtils;
import ezvcard.VCard;
import ezvcard.parameter.ImageType;
import ezvcard.property.Photo;
import ezvcard.property.RawProperty;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class VCardServiceImpl extends VCardService {

    private final Context mContext;

    public VCardServiceImpl(Context context) {
        this.mContext = context;
    }

    public static Single<Tuple<String, Object>> loadProfile(@NonNull Account account) {
        synchronized (account) {
            Single<Tuple<String, Object>> ret = account.getLoadedProfile();
            if (ret == null) {
                ret = Single.fromCallable(() -> readData(account.getProfile()))
                        .subscribeOn(Schedulers.computation())
                        .cache();
                account.setLoadedProfile(ret);
            }
            return ret;
        }
    }

    @Override
    public Single<VCard> loadSmallVCard(String accountId, int maxSize) {
        return VCardUtils.loadLocalProfileFromDisk(mContext.getFilesDir(), accountId)
                .filter( vcard -> !VCardUtils.isEmpty(vcard)).toSingle()
                .map(vcard -> {
                    if (!vcard.getPhotos().isEmpty()) {
                        // Reduce photo to fit in maxSize, assuming JPEG compress with ratio of at least 8
                        byte[] data = vcard.getPhotos().get(0).getData();
                        Bitmap photo = BitmapUtils.bytesToBitmap(data, maxSize * 8);
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        photo.compress(Bitmap.CompressFormat.JPEG, 88, stream);
                        vcard.removeProperties(Photo.class);
                        vcard.addPhoto(new Photo(stream.toByteArray(), ImageType.JPEG));
                    }
                    vcard.removeProperties(RawProperty.class);
                    return vcard;
                });
    }

    @Override
    public Single<VCard> saveVCardProfile(String accountId, String uri, String displayName, String picture)
    {
        return Single.fromCallable(() -> VCardUtils.writeData(uri, displayName, Base64.decode(picture, Base64.DEFAULT)))
                .flatMap(vcard -> VCardUtils.saveLocalProfileToDisk(vcard, accountId, mContext.getFilesDir()));
    }

    @Override
    public Single<Tuple<String, Object>> loadVCardProfile(VCard vcard) {
        return Single.fromCallable(() -> readData(vcard));
    }

    @Override
    public Single<Tuple<String, Object>> peerProfileReceived(String accountId, String peerId, File vcard)
    {
        return VCardUtils.peerProfileReceived(mContext.getFilesDir(), accountId, peerId, vcard)
                .map(VCardServiceImpl::readData);
    }

    public static Tuple<String, Object> readData(VCard vcard) {
        return readData(VCardUtils.readData(vcard));
    }

    public static Tuple<String, Object> readData(Tuple<String, byte[]> profile) {
        return new Tuple<>(profile.first, BitmapUtils.bytesToBitmap(profile.second));
    }

    /**
     * Migrates the user's contacts to their individual account folders under the subfolder profiles
     * @param contacts a hash map of the user's contacts
     * @param accountId the directory where the profile is stored
     */
    @Override
    public void migrateContact(Map<String, CallContact> contacts, String accountId) {
        File fileDir = mContext.getFilesDir();
        File legacyProfileFolder = new File(fileDir, "peer_profiles");
        if (!legacyProfileFolder.exists())
            return;

        File[] profiles = legacyProfileFolder.listFiles();

        if (profiles == null)
            return;

        File accountDir = new File(fileDir, accountId);
        File profilesDir = new File(accountDir, "profiles");
        profilesDir.mkdirs();


        for (File profile : profiles) {
            String filename = profile.getName();
            String contactUri = filename.lastIndexOf(".") > 0 ? filename.substring(0, filename.lastIndexOf(".")) : filename;
            if (contacts.containsKey(contactUri)) {
                File destination = new File(profilesDir, filename);
                FileUtils.copyFile(profile, destination);
            }
        }
    }

    /**
     * Migrates the user's vcards and renames them to profile.vcf
     *
     * @param accountIds the list of accounts to migrate
     */
    @Override
    public void migrateProfiles(List<String> accountIds) {
        File fileDir = mContext.getFilesDir();
        File profileDir = new File(fileDir, "profiles");

        if (!profileDir.exists())
            return;

        File[] profiles = profileDir.listFiles();

        if (profiles == null)
            return;

        for (File profile : profiles) {
            for (String account : accountIds) {
                if (profile.getName().equals(account + ".vcf")) {
                    File accountDir = new File(fileDir, account);
                    File newProfile = new File(accountDir, VCardUtils.LOCAL_USER_VCARD_NAME);
                    FileUtils.moveFile(profile, newProfile);
                    break;
                }
            }
        }

        // necessary to delete profiles leftover by deleted accounts
        for (File profile : profiles) {
            profile.delete();
        }


        profileDir.delete();
    }

    /**
     * Deletes the legacy peer_profiles folder
     */
    @Override
    public void deleteLegacyProfiles() {
        File fileDir = mContext.getFilesDir();
        File legacyProfileFolder = new File(fileDir, "peer_profiles");
        File[] profiles = legacyProfileFolder.listFiles();
        if (profiles == null)
            return;

        for (File file : profiles) {
            file.delete();
        }

        legacyProfileFolder.delete();
    }

    @Override
    public Object base64ToBitmap(String base64) {
        return BitmapUtils.base64ToBitmap(base64);
    }
}
