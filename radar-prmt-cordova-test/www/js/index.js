/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

// Wait for the deviceready event before using any of Cordova's device APIs.
// See https://cordova.apache.org/docs/en/latest/cordova/events/events.html#deviceready
document.addEventListener('deviceready', onDeviceReady, false);

var RadarPassivePlugin = require('cordova-plugin-radar-passive/www/js')

function onDeviceReady() {
    // Cordova is now initialized. Have fun!

    console.log('Running cordova-' + cordova.platformId + '@' + cordova.version);
    document.getElementById('deviceready').classList.add('ready');

    var config = {
        //contact_email: "myemail@email.com",
        //contact_phone: "+31 123456789",
        device_services_to_connect: null,
        empatica_api_key: null,
        enable_bluetooth_requests: "false",
        external_user_id: "peyman-6-android",
        kafka_clean_rate: "3600",
        kafka_records_send_limit: "700",
        kafka_rest_proxy_url: "https://radar-k3s-test.thehyve.net/kafka/",
        kafka_upload_minimum_battery_level: "0.1",
        kafka_upload_rate: "10",
        last_update_check_timestamp: "1683190800000",
        management_portal_url: "https://radar-k3s-test.thehyve.net/managementportal/",
        max_cache_size_bytes: "450000000",
        oauth2_authorize_url: "https://radar-k3s-test.thehyve.net/managementportal/oauth/authorize",
        oauth2_client_id: "pRMT",
        oauth2_client_secret: "saturday$SHARE$scale",
        oauth2_redirect_url: "org.radarcns.detail://oauth2/redirect",
        oauth2_token_url: "https://radar-k3s-test.thehyve.net/managementportal/oauth/token",
        plugins: "opensmile_audio phone_sensors application_status weather phone_location phone_bluetooth phone_contacts phone_usage bittium_faros empatica_e4",
        privacy_policy: "https://www.thehyve.nl/radar-cns-privacy-policy",
        radar_base_url: "https://radar-k3s-test.thehyve.net",
        radar_project_id: "project-test-10",
        radar_user_id: "92ec9fee-516d-43e5-a981-b418a9c9cc63",
        readable_user_id: "92ec9fee-516d-43e5-a981-b418a9c9cc63",
        schema_registry_url: "https://radar-k3s-test.thehyve.net/schema/",
        send_only_with_wifi: "true",
        sender_connection_timeout: "20",
        start_at_boot: "true",
        test_name: "test_value",
        test_name_2: "test_value_2",
        ui_refresh_rate_millis: "250",
        unsafe_kafka_connection: "false"
    };

    setTimeout(function() {
        RadarPassivePlugin.start({
            success: function () {
                console.log('2started!')

                setTimeout(function() {
                    RadarPassivePlugin.configure(config, {
                        success: function (configureRes) {
                            console.log('Configured successfully! configureRes:', configureRes)

                            RadarPassivePlugin.setAuthentication({
                                baseUrl: "https://radar-k3s-test.thehyve.net",
                                // token: 'eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJhdWQiOlsicmVzX2FwcGNvbmZpZyIsInJlc19nYXRld2F5IiwicmVzX01hbmFnZW1lbnRQb3J0YWwiXSwic3ViIjoiOTJlYzlmZWUtNTE2ZC00M2U1LWE5ODEtYjQxOGE5YzljYzYzIiwic291cmNlcyI6WyJkY2Q2YzcxOC02OTFhLTQxMzYtYmYwOC04NzJjZTdjNmE2NzEiLCI3ODIwMWZmMy0wMTY5LTRlMmYtYWMzNS0xM2ZkYmRlZDdlZWMiLCJjY2NiNGE5ZC1iZDgxLTQyZmEtYmVkYy0yMmEzMDc1YjVmMDIiXSwiZ3JhbnRfdHlwZSI6ImF1dGhvcml6YXRpb25fY29kZSIsInVzZXJfbmFtZSI6IjkyZWM5ZmVlLTUxNmQtNDNlNS1hOTgxLWI0MThhOWM5Y2M2MyIsInJvbGVzIjpbInByb2plY3QtdGVzdC0xMDpST0xFX1BBUlRJQ0lQQU5UIl0sInNjb3BlIjpbIk1FQVNVUkVNRU5ULkNSRUFURSIsIlNVQkpFQ1QuUkVBRCIsIlNVQkpFQ1QuVVBEQVRFIl0sImlzcyI6Ik1hbmFnZW1lbnRQb3J0YWwiLCJleHAiOjE2ODMxNTAzODMsImlhdCI6MTY4MzEwNzE4MywiYXV0aG9yaXRpZXMiOlsiUk9MRV9QQVJUSUNJUEFOVCJdLCJjbGllbnRfaWQiOiJwUk1UIn0.EUh03i41rBRMpqnaUsf4fnB5I0m0_A0Nvxo846a2xUMQwfSi0yZ9uuv4q_O2dTOm7pVAhKwL5er7UmlfCG4S6w',
                                token: 'eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJhdWQiOlsicmVzX2FwcGNvbmZpZyIsInJlc19nYXRld2F5IiwicmVzX01hbmFnZW1lbnRQb3J0YWwiXSwic3ViIjoiOTJlYzlmZWUtNTE2ZC00M2U1LWE5ODEtYjQxOGE5YzljYzYzIiwic291cmNlcyI6WyJkY2Q2YzcxOC02OTFhLTQxMzYtYmYwOC04NzJjZTdjNmE2NzEiLCI3ODIwMWZmMy0wMTY5LTRlMmYtYWMzNS0xM2ZkYmRlZDdlZWMiLCJjY2NiNGE5ZC1iZDgxLTQyZmEtYmVkYy0yMmEzMDc1YjVmMDIiXSwiZ3JhbnRfdHlwZSI6ImF1dGhvcml6YXRpb25fY29kZSIsInVzZXJfbmFtZSI6IjkyZWM5ZmVlLTUxNmQtNDNlNS1hOTgxLWI0MThhOWM5Y2M2MyIsInJvbGVzIjpbInByb2plY3QtdGVzdC0xMDpST0xFX1BBUlRJQ0lQQU5UIl0sInNjb3BlIjpbIk1FQVNVUkVNRU5ULkNSRUFURSIsIlNVQkpFQ1QuUkVBRCIsIlNVQkpFQ1QuVVBEQVRFIl0sImlzcyI6Ik1hbmFnZW1lbnRQb3J0YWwiLCJleHAiOjE2ODMyMzY3ODksImlhdCI6MTY4MzE5MzU4OSwiYXV0aG9yaXRpZXMiOlsiUk9MRV9QQVJUSUNJUEFOVCJdLCJjbGllbnRfaWQiOiJwUk1UIn0.357o7TLjEItuN8NS_i7F_L5G3qEU0Qs8f07boS3AkBCBWTEmYPesFWYN55cilPso6E_5ZWfAQA1eLKORnFF1Ng',
                                projectId: "project-test-10",
                                userId: "92ec9fee-516d-43e5-a981-b418a9c9cc63"
                            }, {
                                success: function (authRes) {
                                    console.log('setAuthentication successfully! authRes:', authRes)
                                    setTimeout(function () {
                                        RadarPassivePlugin.serverStatus({
                                            success: function (serverStatus) {
                                                console.log('Configured successfully! serverStatus', serverStatus)
                                            }
                                        })
                                        RadarPassivePlugin.startScanning({
                                            success: function () {
                                                console.log("Started scanning!")
                                            }
                                        })
                                        RadarPassivePlugin.permissionsNeeded({
                                            success: function (permissionsNeeded) {
                                                console.log("permissions needed: ", JSON.stringify(permissionsNeeded))
                                            }
                                        })
                                    }, 1000)
                                }
                            })
                        }
                    })
                }, 1000);
            }
        })
    }, 1000);
}
