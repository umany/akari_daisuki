
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

@SuppressWarnings("serial")
public class AprilFool2017Reply extends HttpServlet {

	private static final Logger log = Logger.getLogger(AprilFool2017Reply.class
			.getName());

	// private static String wikiStatus;

	@SuppressWarnings("unchecked")
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		Twitter twitter = new TwitterFactory().getInstance();

		Random rnd = new Random();

		// reply
		long repSinceID = 1;
		Cache cache = AkariUtil.getCache();

		// キャッシュからrepSinceIDを取得
		if (cache.get("repSinceID") != null) {
			repSinceID = (Long) cache.get("repSinceID");
		}

		String love = null;

		if (cache.get("keyPhraseList") != null) {
			List<String> loves = (List<String>) cache.get("keyPhraseList");
			love = loves.get(rnd.nextInt(loves.size()));
		}

		if (love == null)
			return;

		Paging paging = new Paging(1, 1, repSinceID);
		try {
			List<Status> mentions = twitter.getMentionsTimeline(paging);
			for (Status mention : mentions) {
				StatusUpdate latestStatus = new StatusUpdate("@"
						+ mention.getUser().getScreenName()
						+ " "
						+ String.format("あなたは…………　%sです！", love));
				latestStatus.inReplyToStatusId(mention.getId());
				twitter.updateStatus(latestStatus);
			}
			cache.put("repSinceID", mentions.get(0).getId());
		} catch (TwitterException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
			log.warning(e.getErrorMessage());
		}
	}
}
