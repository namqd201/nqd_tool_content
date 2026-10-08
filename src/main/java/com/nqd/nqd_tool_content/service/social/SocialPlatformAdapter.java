package com.nqd.nqd_tool_content.service.social;

import com.nqd.nqd_tool_content.service.social.dto.DiscoveredAccount;
import com.nqd.nqd_tool_content.service.social.dto.PostResult;
import com.nqd.nqd_tool_content.service.social.dto.PublishRequest;
import com.nqd.nqd_tool_content.service.social.dto.RefreshTokenResult;

import java.time.Instant;
import java.util.List;

public interface SocialPlatformAdapter {

    String getPlatformCode(); // "FACEBOOK", "THREADS", "X", "LINKEDIN"

    List<DiscoveredAccount> discoverAccounts(String accessToken);

    PostResult publish(PublishRequest request);

    boolean validateToken(String accessToken);

    RefreshTokenResult refreshToken(String refreshToken);

    /**
     * Tra cứu bài viết đã đăng trên nền tảng (phục vụ đối soát Reconciler).
     * Trả về PostResult với status:
     * - "PUBLISHED" nếu tìm thấy bài viết đã đăng
     * - "FAILED" nếu chắc chắn bài viết chưa được đăng
     * - "NEEDS_RECONCILE" nếu không thể tra cứu hoặc nền tảng không hỗ trợ
     */
    default PostResult findPublishedPost(PublishRequest request, Instant since) {
        return PostResult.needsReconcile("LOOKUP_UNAVAILABLE: Nền tảng chưa hỗ trợ tra cứu bài đăng", null);
    }

    /**
     * Cho biết nền tảng này có hỗ trợ API đối soát tra cứu bài đã đăng hay không.
     */
    default boolean supportsPublishedPostLookup() {
        return false;
    }

    /**
     * Thu hồi token phía nền tảng khi ngắt kết nối (best-effort).
     */
    default void revokeToken(String token) {
        // Mặc định không làm gì nếu nền tảng không có API thu hồi
    }
}
