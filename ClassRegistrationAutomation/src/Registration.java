import java.io.IOException; 
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import javax.mail.*;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;


//Write pre schedule and results
//Write classes to drop


public class Registration {

	private static String EMAIL_ADDRESS = "micylt727@gmail.com";
	private static String EMAIL_IMAP = "smtp.gmail.com";
	private static String EMAIL_PASSWORD = "********";

	private static WebClient webClient;
	private static Scanner in;
	private static HtmlPage currentPage;
	private static int classesEnrolledIn;

	public static void main(String[] args) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		System.setProperty("java.net.preferIPv4Stack", "true");
		in = new Scanner(System.in);
		login();
		//Need to call scheduleToString() before registering to count classes
		System.out.println("Current Schedule:");
		String pre = scheduleToString();
		System.out.println(pre);
		emailListen();
		enterClasses();
		System.out.println("New Schedule:");
		String post = scheduleToString();
		System.out.println(post);
		if (!pre.equals(post)) {
			System.out.println("Success!");
		} else {
			System.out.println("Failed");
		}
	}

	//Returns string representing schedule as well as counts the number of classes
	//enrolled in
	public static String scheduleToString() {
		String s = currentPage.getWebResponse().getContentAsString();
		String schedule = "";
		while (s.contains("<TD><TT><A")) {
			s = s.substring(s.indexOf("<INPUT TYPE=HIDDEN NAME=credits") + 20);
			s = s.substring(s.indexOf("<TT>") + 4);
			String sln = (s.substring(0, s.indexOf("<")));
			s = s.substring(s.indexOf("<"));
			s = s.substring(s.indexOf("<TT>") + 4);
			String classShort = (s.substring(0, s.indexOf("<")));
			s = s.substring(s.indexOf("<"));
			s = s.substring(s.indexOf("<TD><TT><A") + 10);
			s = s.substring(s.indexOf(">") + 1);
			String className = (s.substring(0, s.indexOf("<")));
			s = s.substring(s.indexOf("<"));
			s = s.substring(s.indexOf("<TD NOWRAP>") + 12);
			s = s.substring(s.indexOf(">") + 1);
			String classDay = (s.substring(0, s.indexOf("<")));
			s = s.substring(s.indexOf("<"));
			s = s.substring(s.indexOf("<TD NOWRAP>") + 12);
			s = s.substring(s.indexOf(">") + 1);
			String classTime = (s.substring(0, s.indexOf("<")));
			String blank = "&nbsp;";
			classTime = classTime.replaceAll(blank, "");
			s = s.substring(s.indexOf("<"));
			schedule += classShort + " " + className + " " + classDay + " " + classTime + "\n";
			classesEnrolledIn++;
		}
		return schedule;
	}


	//Logs into Registration
	public static void login() throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		System.out.print("UWNetID: ");
		String user = in.next();
		System.out.print("UWNetPassword: ");
		String pass = in.next();
		System.out.println();

		webClient = new WebClient();

		currentPage = webClient.getPage("https://sdb.admin.washington.edu/students/uwnetid/register.asp");

		//Get the query input text
		HtmlInput username = currentPage.getElementByName("user");
		username.setValueAttribute(user);

		HtmlInput password = currentPage.getElementByName("pass");
		password.setValueAttribute(pass);

		//Submit the form by pressing the submit button
		HtmlSubmitInput submitBtn = currentPage.getElementByName("submit");
		currentPage = submitBtn.click();

		try {
			currentPage.getElementByName("regform");
		} catch (Exception e) {
			System.out.println("Unsuccessful Login, Try Again");
			System.out.println();
			login();
		}
	}

	public static void emailListen() {
		Properties props = new Properties();
		props.setProperty("mail.store.protocol", "imaps");
		try {
			Session session = Session.getInstance(props, null);
			Store store = session.getStore();
			store.connect(EMAIL_IMAP, EMAIL_ADDRESS, EMAIL_PASSWORD);
			Folder inbox = store.getFolder("INBOX");
			inbox.open(Folder.READ_ONLY);

			inbox.addMessageCountListener(new MessageCountAdapter() {

				public void messagesAdded(MessageCountEvent ev) {
					//Just waiting for email
				}
			});

			// Check mail once in "freq" MILLIseconds
			int freq = 2000; // one second = 1000
			Message msg = inbox.getMessage(inbox.getMessageCount());
			Address[] from = msg.getFrom();
			//while (msg.getSubject() == null || !(msg.getSubject()).contains("SLN")) {
			System.out.println("Listening for email...");
			while (!from[0].toString().contains("notify-noreply")) {
				Thread.sleep(freq); // sleep for freq milliseconds
				msg = inbox.getMessage(inbox.getMessageCount());
				from = msg.getFrom();
			}
			Multipart mp = (Multipart) msg.getContent();
			BodyPart bp = mp.getBodyPart(0);
			String content = bp.getContent().toString();
			//Find the SLN in the email
			String sln = content.substring(content.indexOf(':') + 2, content.indexOf(')'));
			//Register for the sln
			if (sln.equals("12687")) {
				msg = inbox.getMessage(inbox.getMessageCount() - 1);
				mp = (Multipart) msg.getContent();
				bp = mp.getBodyPart(0);
				content = bp.getContent().toString();
				//Find the SLN in the email
				sln = content.substring(content.indexOf(':') + 2, content.indexOf(')'));
			}
			login();
			enterClassesCSE154(sln);

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}


	public static void enterClasses()
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {

		System.out.print("How many SLN's to enter? ");
		int numberOfSLN = in.nextInt();
		List<Integer> slns = new ArrayList<Integer>();
		for (int i = 1; i <= numberOfSLN; i++) {
			System.out.print("SLN #" + i + ": ");
			slns.add(in.nextInt());
		}
		System.out.println("Runnning...");

		HtmlForm form = currentPage.getElementByName("regform");

		//Started at 2 just for user error safety
		int count = 2;
		for (Integer sln : slns) {
			HtmlInput s = currentPage.getElementByName("sln" + (classesEnrolledIn + count));
			s.setValueAttribute("" + sln);
			count++;
		}

		//-------------Droping 5th and 6th class in list------------//
		//		HtmlCheckBoxInput input = currentPage.getElementByName("action5");
		//        input.setChecked(true);
		//        
		//        HtmlCheckBoxInput input2 = currentPage.getElementByName("action6");
		//        input2.setChecked(true);

		HtmlButton submitButton = (HtmlButton)currentPage.createElement("button");
		submitButton.setAttribute("type", "submit");
		form.appendChild(submitButton);
		currentPage = submitButton.click();
		System.out.println("Done!");
	}

	public static void enterClasses(String sln)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {

		System.out.println("Runnning...");

		HtmlForm form = currentPage.getElementByName("regform");

		//Started at 2 just for user error safety
		int count = 2;

		HtmlInput s = currentPage.getElementByName("sln" + (classesEnrolledIn + count));
		s.setValueAttribute(sln);



		//-------------Droping 5th and 6th class in list------------//
		//		HtmlCheckBoxInput input = currentPage.getElementByName("action5");
		//        input.setChecked(true);
		//
		//        HtmlCheckBoxInput input2 = currentPage.getElementByName("action6");
		//        input2.setChecked(true);

		HtmlButton submitButton = (HtmlButton)currentPage.createElement("button");
		submitButton.setAttribute("type", "submit");
		form.appendChild(submitButton);
		currentPage = submitButton.click();
	}

	public static void enterClassesCSE154(String sln)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {

		System.out.println("Runnning...");

		HtmlForm form = currentPage.getElementByName("regform");

		//Started at 2 just for user error safety
		int count = 2;

		HtmlInput s = currentPage.getElementByName("sln" + (classesEnrolledIn + count));
		s.setValueAttribute("12687");
		count++;
		HtmlInput s2 = currentPage.getElementByName("sln" + (classesEnrolledIn + count));
		s2.setValueAttribute(sln);

		//-------------Droping 1st and 2nd class in list------------//
		HtmlCheckBoxInput input = currentPage.getElementByName("action1");
		input.setChecked(true);

		HtmlCheckBoxInput input2 = currentPage.getElementByName("action2");
		input2.setChecked(true);

		HtmlButton submitButton = (HtmlButton)currentPage.createElement("button");
		submitButton.setAttribute("type", "submit");
		form.appendChild(submitButton);
		currentPage = submitButton.click();

	}
}