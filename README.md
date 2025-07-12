# ERD
![ERDiagram](./readme_img/ERD.png)
---
## 이슈
### ✔ Soft Delete 여부 검토

- 예: `users`, `workspaces`, `works` 등에서 실제 삭제 대신 `is_deleted` 컬럼으로 관리 → 추적성, 복구 가능