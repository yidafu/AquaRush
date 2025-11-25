# Frontend Project Structure

This project contains 3 frontend applications: User Mini Program, Delivery Mini Program, and Admin Dashboard.

## 📱 1. User Mini Program (user-client)

### Tech Stack
- **Framework**: Remax (React for WeChat Mini Program)
- **Language**: TypeScript + React
- **Build Tool**: Remax CLI

### Page Structure
```
src/
├── pages/
│   ├── home/              # Home - Product list
│   ├── product-detail/    # Product details
│   ├── order-confirm/     # Order confirmation
│   ├── my/                # My profile
│   ├── address-list/      # Address list
│   ├── address-edit/      # Address editing
│   ├── order-list/        # Order list
│   └── order-detail/      # Order details
├── components/            # Shared components
├── services/              # API services
│   ├── product.ts        # Product API
│   └── order.ts          # Order API
├── utils/                 # Utility functions
│   └── request.ts        # Request wrapper
├── app.tsx               # App entry
└── app.config.ts         # Mini program config
```

### Start Commands
```bash
cd frontend/user-client
npm install
npm run dev
```

---

## 🚚 2. Delivery Mini Program (delivery-client)

### Tech Stack
- **Framework**: Remax
- **Language**: TypeScript + React

### Page Structure
```
src/
├── pages/
│   ├── task-list/         # Delivery task list
│   ├── task-detail/       # Task details
│   ├── delivery-confirm/  # Delivery confirmation
│   ├── history/           # History records
│   └── my/                # My profile
├── services/
│   └── delivery.ts       # Delivery API
└── utils/
    └── request.ts        # Request wrapper
```

### Core Features
- ✅ Online/Offline status toggle
- ✅ Delivery task list
- ✅ Map navigation
- ✅ Photo confirmation on delivery
- ✅ Delivery history

### Start Commands
```bash
cd frontend/delivery-client
npm install
npm run dev
```

---

## 💻 3. Admin Dashboard (admin-client)

### Tech Stack
- **Framework**: React 18 + Vite
- **UI Library**: Ant Design 5
- **Router**: React Router 6
- **HTTP Client**: Axios
- **Language**: TypeScript

### Page Structure
```
src/
├── pages/
│   ├── login/            # Login page
│   ├── dashboard/        # Dashboard
│   ├── products/         # Product management
│   ├── orders/           # Order management
│   ├── delivery/         # Delivery management
│   └── statistics/       # Revenue statistics
├── components/
│   └── Layout.tsx       # Layout component
├── services/
│   ├── product.ts       # Product API
│   └── order.ts         # Order API
├── utils/
│   └── request.ts       # Axios wrapper
├── App.tsx              # Route configuration
└── main.tsx             # App entry
```

### Core Features
- ✅ Login authentication
- ✅ Product management (CRUD)
- ✅ Order management (view, assign delivery)
- ✅ Delivery personnel management
- ✅ Revenue statistics & reports

### Start Commands
```bash
cd frontend/admin-client
npm install
npm run dev         # Development: http://localhost:3000
npm run build       # Production build
```

---

## 🔧 Development Guide

### API Configuration
All frontend projects default to API endpoint `http://localhost:8080/api`

#### User/Delivery Mini Programs
Modify `src/utils/request.ts`:
```typescript
const API_BASE_URL = 'http://localhost:8080/api';
```

#### Admin Dashboard
Modify proxy configuration in `vite.config.ts`:
```typescript
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true
  }
}
```

### TypeScript Errors
TypeScript errors in created files are expected because:
1. Dependencies not yet installed (`npm install`)
2. These are placeholder code that need implementation based on actual requirements

### TODO Features
- [ ] User login (WeChat authorization)
- [ ] Product list loading and display
- [ ] Order creation and payment
- [ ] Complete address management flow
- [ ] Delivery task assignment and confirmation
- [ ] Charts and data visualization
- [ ] File upload (product images, delivery photos)

---

## 📦 Install Dependencies

Before using, install dependencies for each project:

```bash
# User mini program
cd frontend/user-client && npm install

# Delivery mini program
cd frontend/delivery-client && npm install

# Admin dashboard
cd frontend/admin-client && npm install
```

---

## 🎯 Next Steps
1. Install dependencies for each project
2. Start backend service (Spring Boot)
3. Configure WeChat Mini Program AppID
4. Implement specific business logic
5. Integrate and test APIs
