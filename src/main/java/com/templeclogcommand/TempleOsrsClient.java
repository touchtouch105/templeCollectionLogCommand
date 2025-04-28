/*
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.templecollectionlog;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import javax.inject.Inject;

import net.runelite.http.api.RuneLiteAPI;
import net.runelite.http.api.chat.Duels;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TempleOsrsClient
{
    private final OkHttpClient client;
    private final HttpUrl apiBase = HttpUrl.get("https://templeosrs.com/api");
    private final Gson gson;

    @Inject
    private TempleOsrsClient(OkHttpClient client, Gson gson)
    {
        this.client = client;
        this.gson = gson;
    }

    public boolean submitCollectionLogList(String username, Collection<Integer> clogList, String bossName ) throws IOException
    {
//        bossName = "abyssal_sire";
//        HttpUrl url = apiBase.newBuilder()
//                .addPathSegment("collection-log")
//                .addPathSegment("player_collection_log.php")
//                .addQueryParameter("player", username )
//                .addQueryParameter( "categories", bossName )
//                .build();
//
//        Request request = new Request.Builder()
//                .post(RequestBody.create(RuneLiteAPI.JSON, gson.toJson( clogList ) ))
//                .url(url)
//                .build();
//
//        try (Response response = client.newCall(request).execute())
//        {
//            return response.isSuccessful();
//        }
        return true;
    }

    public String getCollectionLogList(String username, String bossName ) throws IOException
    {
        HttpUrl url = apiBase.newBuilder()
                .addPathSegment("collection-log")
                .addPathSegment("player_collection_log.php")
                .addQueryParameter("player", username )
                .addQueryParameter( "categories", bossName )
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute())
        {
            if (!response.isSuccessful())
            {
                throw new IOException("Unable to look up clog list!");
            }

            byte[] in = response.body().bytes();
            String responseString = new String(in, StandardCharsets.UTF_8 );
            return responseString;
            // CHECKSTYLE:ON
        }
        catch (JsonParseException ex)
        {
            throw new IOException(ex);
        }
    }
}
