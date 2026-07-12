# AI Document Analysis & Management API

## What it does
Upload a PDF, get back structured AI-generated analysis (document type, key topics, summary), and manage the results through standard REST endpoints.

## How it works
1. PDF is uploaded via a multipart POST request
2. Apache PDFBox extracts raw text from the PDF server-side
3. Extracted text (capped at 3000 characters) is sent to Groq's LLM API with a system prompt that forces structured JSON output
4. The response is parsed into typed fields (`documentType`, `keyTopics`, `summary`) and persisted via Spring Data JPA/Hibernate to MySQL
5. Full CRUD is exposed over the stored records

## API Endpoints
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/documents/upload` | Upload a PDF, get back AI-generated analysis |
| GET | `/api/documents` | List all processed documents |
| GET | `/api/documents/{id}` | Get one document by id |
| DELETE | `/api/documents/{id}` | Delete a document record |

## Running Locally
1. Clone the repo, open in IntelliJ (Java 21 required)
2. Create a MySQL database: `document_processing_db`
3. Set environment variables: `DB_USERNAME`, `DB_PASSWORD`, `GROQ_API_KEY` (free key at [console.groq.com](https://console.groq.com))
4. Run `DocumentProcessingApplication.java`
5. App runs on `http://localhost:8081`

## Testing
Tested end-to-end via Postman: uploaded a real PDF and verified the structured JSON response (`documentType`/`keyTopics`/`summary`), confirmed list/get/delete endpoints work correctly, and verified error handling (non-PDF upload returns `400`, unknown id returns `404`).

## Possible Future Improvements
- Chunking for documents longer than 3000 characters instead of truncating
- File size validation before reading the file into memory
- Pagination on the list endpoint
