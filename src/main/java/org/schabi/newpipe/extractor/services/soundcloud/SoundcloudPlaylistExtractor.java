package org.schabi.newpipe.extractor.services.soundcloud;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import org.schabi.newpipe.extractor.Downloader;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;

import javax.annotation.Nonnull;
import java.io.IOException;

@SuppressWarnings("WeakerAccess")
public class SoundcloudPlaylistExtractor extends PlaylistExtractor {
    private String playlistId;
    private JsonObject playlist;

    public SoundcloudPlaylistExtractor(StreamingService service, String url, String nextPageUrl) throws IOException, ExtractionException {
        super(service, url, nextPageUrl);
    }

    @Override
    public void onFetchPage(@Nonnull Downloader downloader) throws IOException, ExtractionException {

        playlistId = getUrlIdHandler().getId(getOriginalUrl());
        String apiUrl = "https://api.soundcloud.com/playlists/" + playlistId +
                "?client_id=" + SoundcloudParsingHelper.clientId() +
                "&representation=compact";

        String response = downloader.download(apiUrl);
        try {
            playlist = JsonParser.object().from(response);
        } catch (JsonParserException e) {
            throw new ParsingException("Could not parse json response", e);
        }
    }

    @Nonnull
    @Override
    public String getCleanUrl() {
        return playlist.isString("permalink_url") ? playlist.getString("permalink_url") : getOriginalUrl();
    }

    @Nonnull
    @Override
    public String getId() {
        return playlistId;
    }

    @Nonnull
    @Override
    public String getName() {
        return playlist.getString("title");
    }

    @Override
    public String getThumbnailUrl() {
        return playlist.getString("artwork_url");
    }

    @Override
    public String getBannerUrl() {
        return null;
    }

    @Override
    public String getUploaderUrl() {
        return SoundcloudParsingHelper.getUploaderUrl(playlist);
    }

    @Override
    public String getUploaderName() {
        return SoundcloudParsingHelper.getUploaderName(playlist);
    }

    @Override
    public String getUploaderAvatarUrl() {
        return SoundcloudParsingHelper.getAvatarUrl(playlist);
    }

    @Override
    public long getStreamCount() {
        return playlist.getNumber("track_count", 0).longValue();
    }

    @Nonnull
    @Override
    public StreamInfoItemsCollector getStreams() throws IOException, ExtractionException {
        StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());

        // Note the "api", NOT "api-v2"
        String apiUrl = "https://api.soundcloud.com/playlists/" + getId() + "/tracks"
                + "?client_id=" + SoundcloudParsingHelper.clientId()
                + "&limit=20"
                + "&linked_partitioning=1";

        nextPageUrl = SoundcloudParsingHelper.getStreamsFromApiMinItems(15, collector, apiUrl);
        return collector;
    }

    @Override
    public InfoItemPage getInfoItemPage() throws IOException, ExtractionException {
        if (!hasNextPage()) {
            throw new ExtractionException("Playlist doesn't have more streams");
        }

        StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());
        nextPageUrl = SoundcloudParsingHelper.getStreamsFromApiMinItems(15, collector, nextPageUrl);

        return new InfoItemPage(collector, nextPageUrl);
    }
}
