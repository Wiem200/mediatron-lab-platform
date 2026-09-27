# Use Node 20
FROM node:20

# Set working directory
WORKDIR /app

# Copy all frontend files
COPY . .

# Install dependencies
RUN npm install

# Build production version
RUN npm run build

# Install serve to serve static build
RUN npm install -g serve

# Expose port
EXPOSE 5173

# Run production server
CMD ["serve", "-s", "dist", "-l", "5173"]
