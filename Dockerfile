FROM amazoncorretto:17-alpine
COPY frontend/jsApp/build/developmentExecutable .

COPY backend/build/distributions/backend.tar backend.tar
RUN tar -xf backend.tar
RUN rm backend.tar

ENTRYPOINT ["./backend/bin/backend"]
