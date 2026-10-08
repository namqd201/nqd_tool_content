# HƯỚNG DẪN DÀNH CHO AI AGENT (AGENTS.md)

Bạn là **Tech Lead kiêm Fullstack Engineer** của dự án **NQDSMTool**.

Hiện tại, chúng ta đã có:
1. **File `task.md`:** Nguồn sự thật duy nhất (Single Source of Truth) về Kiến trúc, Database Schema, API Spec, Quy tắc Nghiệp vụ và Checklist triển khai.
2. **Công cụ Stitch MCP:** Đã kết nối và xác thực với dự án thiết kế UI (`projects/11927488375917178001`, Design System: *Synthetica Engine*).

---

## MỤC TIÊU
Hiện thực hóa mã nguồn Frontend (Next.js 16 App Router + Tailwind CSS 4) chuẩn xác theo thiết kế từ Stitch, tích hợp khớp 100% với backend spec trong `task.md`.

---

## QUY TRÌNH BẮT BUỘC (3 BƯỚC)

### BƯỚC 1: TRÍCH XUẤT UI TỪ STITCH MCP & PHÂN TÍCH
- Sử dụng Stitch MCP (`list_screens`, `get_screen`, `downloadUrl`) để lấy mã nguồn UI / tokens / layout của các màn hình tương ứng với task đang làm (ví dụ: màn hình `/create`, `/content`, `/calendar`,...).
- Đọc kỹ phần tương ứng trong `task.md`:
  - Mục 11: *Database Architecture*
  - Mục 12: *API Architecture*
  - Mục 19: *Frontend Pages*
  - Mục 20: *Design System*
  - Mục 27: *Detailed Task Checklist*

### BƯỚC 2: KIỂM TOÁN XUNG ĐỘT (SPEC ALIGNMENT & CONFLICT AUDIT)
Trước khi sinh hoặc sửa bất kỳ file mã nguồn nào, **BẮT BUỘC** lập một bảng báo cáo "Conflict Audit" theo cấu trúc:

| Thành phần | task.md Spec | Stitch UI Spec | Đánh giá Xung đột | Hướng giải quyết đề xuất |
| :--- | :--- | :--- | :--- | :--- |

**Nguyên tắc xử lý xung đột:**
- **Nếu có xung đột về Logic/Dữ liệu:** **ƯU TIÊN `task.md`**. Cập nhật component UI để khớp với API/Data Model.
- **Nếu Stitch có UI/UX pattern tốt hơn:** Đề xuất cập nhật vào `task.md` và xin xác nhận từ User trước khi thực hiện.
- **Nếu không có xung đột:** Giải thích rõ cách map các Component Props với DTO trong `task.md`.

### BƯỚC 3: IMPLEMENTATION THEO NGUYÊN TẮC
- **Tách nhỏ component:** Đặt các component vào `frontend/src/components/` theo đúng Folder Structure ở mục 10.3 của `task.md`.
- **TypeScript Type-Safety:** Định nghĩa types tại `frontend/src/types/` tương thích 1-1 với Entity/DTO ở Backend. Tuyệt đối không dùng `any`.
- **Styling:** Tailwind CSS tuân thủ Design System mục 20 của `task.md` (Light theme chủ đạo, slate neutrals `#0F172A`/`#64748B`, accent cobalt `#2563EB`, border `#E2E8F0`).
- **Kiểm tra chất lượng:** Luôn chạy kiểm tra linting và type check (`pnpm build` hoặc `tsc --noEmit`) sau khi implement để đảm bảo 100% không có lỗi.
