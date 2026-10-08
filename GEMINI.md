# HƯỚNG DẪN DỰ ÁN & QUY ƯỚC LÀM VIỆC (GEMINI.md)

> [!IMPORTANT]
> **YÊU CẦU BẮT BUỘC DÀNH CHO AI AGENT / ASSISTANT:**
> Mỗi khi bắt đầu một phiên làm việc mới hoặc nhận yêu cầu mới, **BẮT BUỘC ĐỌC FILE NÀY TRƯỚC TIÊN** và tuân thủ nghiêm ngặt các quy tắc dưới đây.

---

## 1. NGUYÊN TẮC BẤT DI BẤT DỊCH (CORE CONSTRAINTS)

1. **Tuyệt đối không sửa đổi phiên bản công nghệ cốt lõi:**
   - **Java / JDK:** 21 (toolchain `languageVersion = JavaLanguageVersion.of(21)`)
   - **Spring Boot:** 4.1.1
   - **Build Tool:** Gradle (Wrapper 9.x)
   - Không được tự ý hạ cấp (downgrade) hoặc nâng cấp (upgrade) phiên bản Spring Boot hay Java trong `build.gradle` trừ khi người dùng có chỉ thị trực tiếp.

2. **Cấu hình Cơ sở dữ liệu (Database Config):**
   - **DBMS:** PostgreSQL
   - **Username:** `postgres`
   - **Password:** `123`
   - **Port:** `5432`
   - **Database Name:** `nqd_tool_content`
   - **URL:** `jdbc:postgresql://localhost:5432/nqd_tool_content`
   - Cấu hình lưu trữ tại `src/main/resources/application.properties`.

3. **Phát triển đồng bộ Fullstack (BE + FE Synchronization):**
   - Dự án gồm Backend (Spring Boot tại `nqd_tool_content`) và Frontend (Next.js tại `frontend`).
   - **Bắt buộc:** Khi được giao bất kỳ task nghiệp vụ nào, phải luôn thực hiện đồng bộ ở cả Backend và Frontend để hệ thống hoạt động liền mạch từ API, DTO, Database cho đến UI/UX người dùng.

4. **Bảo mật dữ liệu nhạy cảm (Security & Sensitive Data):**
   - **Tuyệt đối không** thêm dữ liệu nhạy cảm (API keys, mật khẩu, secrets, tokens,...) vào các file sẽ public/commit lên Git như `application.properties`.
   - **Giải pháp thay thế:** Sử dụng file cấu hình local riêng biệt (ví dụ: `application-local.properties`, `.env`) hoặc biến môi trường (environment variables).
   - **Bảo vệ qua Git:** Bắt buộc phải thêm các file chứa thông tin nhạy cảm/local này vào `.gitignore` để đảm bảo không bao giờ bị commit lên Git repository.

---

## 2. SUY NGHĨ KỸ TRƯỚC KHI VIẾT MÃ (THINK BEFORE CODING)

- **Lập kế hoạch trước:** Lập kế hoạch chi tiết cho bất kỳ tác vụ nào có từ 3 bước trở lên hoặc có ảnh hưởng đến kiến trúc hệ thống. Với cửa sổ ngữ cảnh (context window) lớn, hãy nắm bắt toàn bộ kế hoạch cùng mã nguồn liên quan cùng lúc. Không bắt đầu chỉnh sửa cho đến khi kế hoạch đã cụ thể và rõ ràng.
- **Nêu rõ các giả định:** Nếu không chắc chắn, hãy hỏi. Nếu có nhiều cách hiểu, hãy trình bày tất cả — không tự ý chọn một cách mà không thông báo cho người dùng.
- **Chỉ ra các sự đánh đổi:** Nếu có cách tiếp cận đơn giản hơn, hãy chủ động đề xuất. Nếu yêu cầu có vẻ không hợp lý, hãy phản biện lại trước khi bắt tay vào thực hiện.
- **Nêu rõ những điểm chưa hiểu:** Nếu có điều gì chưa rõ ràng, hãy dừng lại và hỏi ngay. Tuyệt đối không cố che đậy bằng những đoạn mã trông có vẻ hợp lý nhưng thực chất không giải quyết đúng vấn đề.

---

## 3. SỬ DỤNG CỬA SỔ NGỮ CẢNH CÓ CHỦ ĐÍCH (DELIBERATE CONTEXT USAGE)

- Cửa sổ ngữ cảnh lớn là công cụ hỗ trợ, không phải sự cho phép nạp dữ liệu tùy ý. Việc nạp toàn bộ kho mã nguồn (repo) hiếm khi là lựa chọn đúng đắn vì sẽ làm loãng sự tập trung vào những phần thực sự quan trọng.
- **Chỉ đọc những tệp cần thiết:** Cùng với các tệp trực tiếp gọi đến hoặc được gọi bởi chúng. Bỏ qua những phần còn lại.
- **Dò tìm có trọng tâm:** Khi cần tìm hiểu hoặc dò tìm mã, ưu tiên dùng lệnh `grep`/`find` thay vì đọc toàn bộ thư mục. Chỉ nạp tệp vào ngữ cảnh khi đã thu hẹp được phạm vi tìm kiếm.

---

## 4. ƯU TIÊN SỰ ĐƠN GIẢN (KISS & YAGNI)

- Viết lượng mã tối thiểu đủ để giải quyết vấn đề được nêu. Không viết mã dựa trên những dự đoán mơ hồ về tương lai.
- Không thêm tính năng, lớp trừu tượng, tùy chọn cấu hình hay cơ chế xử lý lỗi cho các trường hợp không được yêu cầu.
- Không tạo lớp trừu tượng cho những đoạn mã chỉ dùng một lần.
- **Tiêu chuẩn Senior:** Trước khi gửi mã, hãy tự hỏi: Liệu một kỹ sư cấp cao (senior) có coi cách làm này là quá phức tạp không? Nếu có, hãy viết lại đơn giản hơn.

---

## 5. THAY ĐỔI CÓ TRỌNG TÂM (SURGICAL CHANGES)

- Chỉ tác động vào những phần mà tác vụ yêu cầu. Việc sửa lỗi không phải là cái cớ để tái cấu trúc (refactor) toàn bộ mã nguồn.
- Đừng cố "cải thiện" mã nguồn, chú thích hoặc định dạng của các phần lân cận không liên quan. Hãy tuân thủ phong cách hiện có của dự án.
- Dọn dẹp các thành phần thừa phát sinh từ thay đổi của bạn (như các lệnh `import` không dùng đến, biến không còn được sử dụng).
- Để nguyên các đoạn mã chết (dead code) có sẵn từ trước — thay vào đó, hãy ghi chú lại về chúng.
- Mỗi dòng mã được thay đổi phải có lý do trực tiếp liên quan đến yêu cầu ban đầu.

---

## 6. KIẾN TRÚC PHẦN MỀM (LAYERED ARCHITECTURE)

Dự án áp dụng mô hình phân tầng **Layered Architecture** chuẩn mực cho Spring Boot thuộc root package `com.nqd.nqd_tool_content`:

| Tầng (Package) | Mô tả & Trách nhiệm |
| :--- | :--- |
| `controller` | **Presentation Layer**: Tiếp nhận HTTP request, validate tham số đầu vào (`@Valid`), điều phối và gọi `service`, trả về `ResponseEntity<...>` với mã trạng thái HTTP chuẩn RESTful. |
| `service` | **Business Logic Interface**: Khai báo các interface định nghĩa nghiệp vụ của hệ thống. |
| `service.impl` | **Business Logic Implementation**: Triển khai nghiệp vụ thực tế (`@Service`), quản lý transaction (`@Transactional`), gọi repository/mapper. |
| `repository` | **Data Access Layer**: Kế thừa `JpaRepository<Entity, ID>` để thao tác truy vấn dữ liệu từ PostgreSQL. |
| `entity` | **Persistence Layer / Domain**: Khai báo các JPA Entity (`@Entity`, `@Table`, `@Id`, quan hệ `@ManyToOne`, `@OneToMany`,...) ánh xạ với bảng trong PostgreSQL. |
| `dto.request` | **Data Transfer Objects (Inbound)**: Chứa các class nhận dữ liệu từ client gửi lên API. |
| `dto.response` | **Data Transfer Objects (Outbound)**: Chứa các class định dạng dữ liệu trả về cho client. |
| `config` | **Configuration**: Chứa các cấu hình hệ thống (`SecurityConfig`, `WebConfig`, `CorsFilter`, cấu hình Kafka, Beans,...). |
| `exception` | **Exception Handling**: Chứa các Custom Exception (kế thừa `RuntimeException`) và `GlobalExceptionHandler` (`@RestControllerAdvice`). |
| `mapper` | **Object Mapping**: Chuyển đổi dữ liệu giữa `Entity` và `DTO` (MapStruct hoặc mapper thủ công). |
| `util` | **Utilities & Constants**: Tiện ích dùng chung, constants, enums, helper methods. |

---

## 7. QUY ƯỚC LẬP TRÌNH (CODING STANDARDS)

- **DTO thay vì Entity:** Controller không bao giờ nhận hoặc trả về trực tiếp JPA Entity. Bắt buộc dùng `dto.request` và `dto.response`.
- **Lombok:** Sử dụng `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder` một cách nhất quán.
- **Dependency Injection:** Sử dụng Constructor Injection (`@RequiredArgsConstructor` kết hợp `private final`), tránh dùng `@Autowired` trực tiếp trên field.
- **Xử lý ngoại lệ (Exception Handling):** Ném các ngoại lệ nghiệp vụ cụ thể và xử lý tập trung tại `@RestControllerAdvice`, trả về cấu trúc lỗi chuẩn (gồm timestamp, status, message, errors).
- **Validation:** Áp dụng `jakarta.validation.constraints` (`@NotBlank`, `@NotNull`, `@Size`, `@Email`,...) trên DTO Request và kích hoạt bằng `@Valid` tại Controller.
- **Kiểm thử biên dịch:** Sau khi viết code mới hoặc thay đổi mã nguồn, luôn kiểm tra tính hợp lệ bằng lệnh `./gradlew compileJava` trước khi hoàn tất.

---

## 8. THỰC THI ĐỊNH HƯỚNG THEO MỤC TIÊU (GOAL-DRIVEN EXECUTION)

- Chuyển đổi các nhiệm vụ mơ hồ thành các mục tiêu có thể kiểm chứng trước khi bắt đầu:
  - *"Thêm phần kiểm tra dữ liệu"* → Viết các bài kiểm thử (test) cho dữ liệu đầu vào không hợp lệ, sau đó đảm bảo chúng vượt qua kiểm thử.
  - *"Sửa lỗi"* → Viết một bài kiểm thử thất bại để tái hiện lỗi, sau đó sửa để nó vượt qua kiểm thử.
  - *"Tái cấu trúc X"* → Đảm bảo kiểm thử vẫn vượt qua trước và sau khi sửa, hành vi hệ thống không thay đổi.
- Đối với công việc gồm nhiều bước, hãy lập kế hoạch ngắn gọn kèm bước kiểm chứng cho từng hạng mục.
- Lặp lại quy trình cho đến khi được kiểm chứng. **Không bao giờ đánh dấu hoàn thành nếu thiếu bằng chứng** — hãy chạy kiểm thử, kiểm tra nhật ký (log) và so sánh hành vi.

---

## 9. GIỚI HẠN QUYỀN TỰ CHỦ (AUTONOMY BOUNDARIES)

- **Tự chủ trong khâu thực thi:** Chủ động tìm lỗi, chạy kiểm thử, kiểm tra nhật ký (log), sửa chữa các phần bị lỗi.
- **Không tự chủ trong khâu diễn giải:** Nếu yêu cầu mơ hồ hoặc dựa trên các giả định không thể kiểm chứng, hãy dừng lại và hỏi trước khi viết mã. Những giả định sai lầm sẽ dẫn đến các hệ quả tích tụ nghiêm trọng.

---

## 10. GỌI CÔNG CỤ (TOOL CALLS)

- Ưu tiên thực hiện ít thao tác đọc dữ liệu nhưng quy mô lớn hơn thay vì nhiều thao tác nhỏ lẻ (ví dụ: đọc 5 tệp trong một lần thực hiện sẽ hiệu quả hơn so với 5 lần gọi riêng biệt).
- Không đọc lại tệp đã có trong ngữ cảnh (context) hiện tại. Tin tưởng vào dữ liệu đang hiển thị trong cửa sổ làm việc.
- Khi chạy các lệnh shell, gộp các lệnh liên quan lại với nhau (ví dụ: `cd dir && cmd1 && cmd2`) thay vì gọi riêng lẻ nếu có thể.

---

## 11. TỆP QUY TẮC DÀNH CHO TÁC NHÂN (GEMINI.md / AGENTS.md)

- Tệp này bao gồm: tổng quan dự án, lệnh thiết lập, ghi chú kiến trúc, quy ước và danh sách các phần không được phép thay đổi.
- Nếu các quy tắc dự án áp dụng giống nhau cho tất cả các tác nhân (agent), có thể dùng chung với tệp `AGENTS.md`. Gemini sẽ luôn ưu tiên đọc và tuân thủ các quy tắc trong tệp này.

---

## 12. ĐỊNH DẠNG THÔNG ĐIỆP COMMIT (GIT COMMIT CONVENTION)

- **Subject (Tiêu đề):** `<type>(<scope>): <subject>` — tối đa 72 ký tự, sử dụng câu mệnh lệnh.
  - Các loại (`types`): `feat` | `fix` | `refactor` | `chore` | `docs` | `test`
- **Body (Nội dung chi tiết - tùy chọn):** Mô tả những gì đã thay đổi, lý do thay đổi và các bước hoàn tác (rollback) nếu quy trình phức tạp.
- **Trailers (Thông tin bổ sung):**
```bash
git commit \
  --trailer "Risk-Level: low|medium|high" \
  --trailer "AI-Agent: gemini-2.5-pro" \
  -m "fix(scope): subject line" \
  -m "- Những thay đổi và lý do
- Rollback: hoàn tác commit; mô tả các tác dụng phụ (nếu có)"
```

---

## 13. TỰ KIỂM TRA TRƯỚC KHI XÁC NHẬN HOÀN THÀNH (SELF-CHECKLIST)

Trước khi hoàn tất bất kỳ phản hồi hoặc tác vụ nào, hãy tự trả lời 4 câu hỏi:
1. [ ] **Mỗi dòng thay đổi có tương ứng với yêu cầu ban đầu không?**
2. [ ] **Tôi đã thực sự kiểm chứng hay chỉ phỏng đoán?** (đã chạy test, kiểm tra log, so sánh sự khác biệt về hành vi).
3. [ ] **Tôi đã nêu rõ các giả định, sự đánh đổi hoặc những điểm chưa rõ ràng trước khi viết mã chưa?**
4. [ ] **Một kỹ sư cấp cao (senior) có chấp thuận bản thay đổi (diff) này mà không cần góp ý gì thêm không?**

> **Lưu ý:** Nếu câu trả lời là "KHÔNG" cho bất kỳ mục nào ở trên, hãy khắc phục vấn đề đó trước khi báo cáo hoàn thành cho người dùng.
