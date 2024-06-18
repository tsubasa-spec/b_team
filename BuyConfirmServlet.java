package servlet;

import java.io.IOException;
import java.util.ArrayList;

import bean.Book;
import bean.Order;
import bean.User;
import dao.BookDAO;
import dao.OrderDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import util.SendMail;

@WebServlet("/buyConfirm")
public class BuyConfirmServlet extends HttpServlet {

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String error = "";
		String cmd = "logout";
		BookDAO objDao = new BookDAO();
		OrderDAO orderDao = new OrderDAO();
		ArrayList<Book> bookList = new ArrayList<>();
		try {
			HttpSession session = request.getSession();
			
			// ログインしていなかったらエラー
			User user = (User) session.getAttribute("user");
			if (user == null) {
				error = "セッション切れの為、購入はできません。";
				return;
			}
			
			// カートの中に何も入っていなかったら購入不可
			ArrayList<Order> orderList = (ArrayList<Order>) session.getAttribute("orderList");
			if (orderList == null || orderList.size() == 0) {
				error = "カートの中に何もなかったので購入はできません。";
				cmd = "menu";
				return;
			}
			
			//メール文、合計計算
			String buyList = "";
			int total = 0;
			
			// bookinfoからorderList(カートデータ)分だけ書籍情報を呼び出す。
			for (Order order : orderList) {
				Book book = objDao.selectByIsbn(order.getIsbn());
				// DBのorderinfoに注文情報を登録する
				orderDao.insert(order);
				// 取得したデータをlistに追加
				bookList.add(book);
				
				// メール本文の注文情報を格納
				buyList += book.getIsbn() + "\t"
						+ book.getTitle() + "\t"
						+ book.getPrice() + "円\n";
				//合計金額を計算
				total += book.getPrice();
			}
			
			// メールの件名を作成
			String subject = "【神田書店】ご購入誠にありがとうございます。";
			// メール本文を作成
			String body = user.getUserid() + "様\n\n" 
					+ "本のご購入ありがとうざいます。\n" 
					+ "以下内容でご注文を受け付けましたので、ご連絡致します。\n\n" 
					+ buyList 
					+ "合計　" + total + "円\n\n" 
					+ "またのご利用よろしくお願いします。 ";
			
			// メール送信
			SendMail mail = new SendMail();
			mail.send(subject, body, user.getEmail());
			
			request.setAttribute("book_list", bookList);
			request.setAttribute("total", total);
			// オーダー情報クリア（カートの中を空にする）
			session.setAttribute("orderList", null);
		} catch (IllegalStateException e) {
			error = "DB接続エラーの為、購入はできません。";
		} finally {
			if (error.equals("")) {//エラーなし
				request.getRequestDispatcher("/view/buyConfirm.jsp").forward(request, response);
			} else {//エラーあり
				request.setAttribute("cmd", cmd);
				request.setAttribute("error", error);
				request.getRequestDispatcher("/view/error.jsp").forward(request, response);
			}
		}

	}

}
