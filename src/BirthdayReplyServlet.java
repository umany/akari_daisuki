

import java.io.IOException;
import java.util.Random;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

@SuppressWarnings("serial")
public class BirthdayReplyServlet extends HttpServlet {

	private static final Logger log = Logger
			.getLogger(BirthdayReplyServlet.class.getName());
	private static final String[] template = { "わーい！祝え祝えーっ", "プレゼントちょーだい！",
			"<username>ちゃんありがと～！", "あかり、こんなにしあわせでいいのかなぁ！", "あかり嬉しいっ！",
			"えへへ、<username>ちゃん大好きっ", "わぁい♪♪" };

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

		Paging paging = new Paging(1, 100, repSinceID);
		try {
			ResponseList<Status> mentions = twitter.getMentionsTimeline(paging);
			if (mentions.size() > 0) {
				cache.put("repSinceID", mentions.get(0).getId());

				for (Status mention : mentions) {
					if (mention.getText().matches(
							".*(誕生日|たんじょうび|おめでと|オメデト|好き|だいすき|愛|あいしてる|プレゼント|ハッピーバースデ|happy\\s*birthday).*")) {
						StatusUpdate latestStatus = new StatusUpdate(
								"@"
										+ mention.getUser().getScreenName()
										+ " "
										+ template[rnd.nextInt(template.length)]
												.replaceAll("<username>",
														mention.getUser()
																.getName()));
						latestStatus.inReplyToStatusId(mention.getId());
						twitter.updateStatus(latestStatus);
						//break;
						// log.info("Successfully updated the status to ["
						// + status.getText() + "].");
					}
				}
			}
		} catch (TwitterException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
			log.warning(e.getErrorMessage());
		}
	}
}
