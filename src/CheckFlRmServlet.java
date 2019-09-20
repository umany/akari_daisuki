

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import twitter4j.Friendship;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

/**
 * メンションによる半自動フォローバック/リムーブを実行するクラス<br>
 * 最低限のチェック機構のみを有します<br>
 * フォローバックの場合は要求ユーザがこちらをフォローしているか確認後実行<br>
 * リムーブは何も確認せず容赦なく実行します
 * @author umany
 */
@SuppressWarnings("serial")
public class CheckFlRmServlet extends HttpServlet {

	private static final Logger log = Logger.getLogger(TweetServlet.class
			.getName());

	private static Twitter twitter = new TwitterFactory().getInstance();

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

		long mentionSinceId = 1;
		Cache cache = AkariUtil.getCache();

		// キャッシュがあればmentionSinceIdを取得
		if (cache.get("mentionSinceId") != null) {
			mentionSinceId = (long) cache.get("mentionSinceId");
		}

		//Paging設定 1ページ目、最大200件、mentionSinceId以降のツイートを取得する
		Paging paging = new Paging(1, 200, mentionSinceId);

		//mentionsTimeline取得
		ResponseList<Status> mentions = null;
		try {
			mentions = twitter.getMentionsTimeline(paging);
		} catch (TwitterException e) {
			log.warning("failed to get mentions timeline." + "\n" + e.getErrorMessage() + "\n" + e.getMessage());
		}

		//新着Mentionsはあるか？
		if (mentions.size() > 0) {

			//フォローバック要求ユーザはこちらをフォロー済みか検証するため一旦退避する
			List<Long> reqFollowUserIds = new ArrayList<Long>();

			List<Long> followUserIds = new ArrayList<Long>();
			List<Long> removeUserIds = new ArrayList<Long>();

			//「フォローして」でフォローバック要求ユーザと見なす
			//「リムーブして」でリムーブ要求ユーザと見なす
			for (Status mention : mentions) {
				if (mention.getText().matches("\\A@akari_daisuki[^:alpha:_]*フォローして.*")) {
					reqFollowUserIds.add(mention.getUser().getId());
				} else if (mention.getText().matches("\\A@akari_daisuki[^:alpha:_]*リムーブして.*")) {
					removeUserIds.add(mention.getUser().getId());
				}

				//一度にlookupできるのは100IDまで
				if (reqFollowUserIds.size() >= 100) {
					break;
				}
			}

			//friendships/lookupでこちらをフォロー済みか検証する フォロー済みであればfollowUserIdsに追加
			if (reqFollowUserIds.size() > 0) {

				//ちょうかっこわるいんですけどなんとかなりませんかねOracleさん
				long[] lookupIds = new long[reqFollowUserIds.size()];
				for (int i = 0; i < reqFollowUserIds.size(); i++) {
					lookupIds[i] = reqFollowUserIds.get(i);
				}

				ResponseList<Friendship> friendships = null;
				try {
					friendships = twitter
							.lookupFriendships(lookupIds);
				} catch (TwitterException e) {
					log.warning("failed to lookup friendships." + "\n" + e.getErrorMessage() + "\n" + e.getMessage());
				}
				for (Friendship friendship : friendships) {
					if (friendship.isFollowedBy() && !friendship.isFollowing()) {
						followUserIds.add(friendship.getId());
					}
				}

				//フォローバック対象ユーザをフォロー
				for (long followUserId : followUserIds) {
					try {
						twitter.createFriendship(followUserId);
					} catch (TwitterException e) {
						log.warning("failed to follow user. userID:" + followUserId + "\n" + e.getErrorMessage() + "\n"
								+ e.getMessage());
					}
				}
			}

			//リムーブ要求ユーザをリムーブ
			for (long removeUserId : removeUserIds) {
				try {
					twitter.destroyFriendship(removeUserId);
				} catch (TwitterException e) {
					log.warning("failed to remove user. userID:" + removeUserId + "\n" + e.getErrorMessage() + "\n"
							+ e.getMessage());
				}
			}

			log.info("followed " + followUserIds.size() + "user(s).");
			log.info("removed " + removeUserIds.size() + "user(s).");

			//キャッシュに最新のMention Status IDを書き込んで終わり
			@SuppressWarnings({ "unused", "unchecked" })
			Object o = cache.put("mentionSinceId", mentions.get(0).getId());
		}
	}
}
