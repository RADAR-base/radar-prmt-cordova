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
        plugins: "health_connect",
        // plugins: "opensmile_audio phone_sensors application_status weather phone_location phone_bluetooth phone_contacts phone_usage bittium_faros empatica_e4",
        health_connect_data_types: "Steps",
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
                                // token: 'eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJhdWQiOlsicmVzX2FwcGNvbmZpZyIsInJlc19nYXRld2F5IiwicmVzX01hbmFnZW1lbnRQb3J0YWwiXSwic3ViIjoiOTJlYzlmZWUtNTE2ZC00M2U1LWE5ODEtYjQxOGE5YzljYzYzIiwic291cmNlcyI6WyJkY2Q2YzcxOC02OTFhLTQxMzYtYmYwOC04NzJjZTdjNmE2NzEiLCI3ODIwMWZmMy0wMTY5LTRlMmYtYWMzNS0xM2ZkYmRlZDdlZWMiLCJjY2NiNGE5ZC1iZDgxLTQyZmEtYmVkYy0yMmEzMDc1YjVmMDIiXSwiZ3JhbnRfdHlwZSI6ImF1dGhvcml6YXRpb25fY29kZSIsInVzZXJfbmFtZSI6IjkyZWM5ZmVlLTUxNmQtNDNlNS1hOTgxLWI0MThhOWM5Y2M2MyIsInJvbGVzIjpbInByb2plY3QtdGVzdC0xMDpST0xFX1BBUlRJQ0lQQU5UIl0sInNjb3BlIjpbIk1FQVNVUkVNRU5ULkNSRUFURSIsIlNVQkpFQ1QuUkVBRCIsIlNVQkpFQ1QuVVBEQVRFIl0sImlzcyI6Ik1hbmFnZW1lbnRQb3J0YWwiLCJleHAiOjE2ODMyMzY3ODksImlhdCI6MTY4MzE5MzU4OSwiYXV0aG9yaXRpZXMiOlsiUk9MRV9QQVJUSUNJUEFOVCJdLCJjbGllbnRfaWQiOiJwUk1UIn0.357o7TLjEItuN8NS_i7F_L5G3qEU0Qs8f07boS3AkBCBWTEmYPesFWYN55cilPso6E_5ZWfAQA1eLKORnFF1Ng',
                                token: 'eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsicmVzX2FwcGNvbmZpZyIsInJlc19nYXRld2F5IiwicmVzX01hbmFnZW1lbnRQb3J0YWwiXSwic291cmNlcyI6WyI1ZmJjZmIwOS05ZmVhLTQ0NGItODQxZS1lYWVhYTJiZDQ4YjMiLCI2OGI3OThmMC1lODU0LTQ1YjgtOTc4My1kYjgwNzFkYmQ5YTciLCI3ODAzZTEyYi1lOWU4LTRiODktYjM3MS1iYWQ3YzQxMTI0ZWEiLCI1ZDQ2YWEyYy00ZmNkLTQzZmMtODY1Zi1mY2ViOWYzNGEzN2EiLCJkZmQyMDRlNy04YWFhLTQ5NzMtYTk0ZC1kZGQ0YjYxNzNjZjkiLCIyMjM2NzVlNC0xYTVkLTRjODctYWYxZi1hNzM5Y2JkZjBhYTQiXSwicm9sZXMiOlsicHJvamVjdC10ZXN0LTEwOlJPTEVfUEFSVElDSVBBTlQiXSwiYXV0aG9yaXRpZXMiOlsiUk9MRV9QQVJUSUNJUEFOVCJdLCJzY29wZSI6WyJNRUFTVVJFTUVOVC5DUkVBVEUiLCJTVUJKRUNULlJFQUQiLCJTVUJKRUNULlVQREFURSJdLCJzdWIiOiIzODA5MDdjMy1iYjdmLTQwOGUtYmQzYi01MjUzZjk2NzM1ZWYiLCJpc3MiOiJNYW5hZ2VtZW50UG9ydGFsIiwidXNlcl9uYW1lIjoiMzgwOTA3YzMtYmI3Zi00MDhlLWJkM2ItNTI1M2Y5NjczNWVmIiwiY2xpZW50X2lkIjoicFJNVCIsImdyYW50X3R5cGUiOiJhdXRob3JpemF0aW9uX2NvZGUiLCJleHAiOjE2ODgwODM2NzksImlhdCI6MTY4ODA0MDQ3OX0.c-X9rurQjTNQ6kgIE0Vmk3HXnFeGgIakzLwKi7BHsnjzW-N6BAdiXlC7nSfCu_NcgBE65BAIDIpM0so3WqyayA',
                                projectId: "project-test-10",
                                userId: "380907c3-bb7f-408e-bd3b-5253f96735ef"
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
                                            success: function(permissionsNeeded) {
                                                checkPermissions(Object.keys(permissionsNeeded))
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

    function checkPermissions(permissionsNeeded) {
        console.log("permissions needed: ", JSON.stringify(permissionsNeeded))
        RadarPassivePlugin.requestPermissionsSupported({
            success: function (permissionsSupportedSets) {
                var permissionRequestSets = {}
                var supportedSetsKeys = Object.keys(permissionsSupportedSets);
                outer: for (var i = 0; i < permissionsNeeded.length; i++) {
                    var permission = permissionsNeeded[i];
                    for (var j = 0; j < supportedSetsKeys.length; j++) {
                        var permissionSupportedSet = permissionsSupportedSets[supportedSetsKeys[j]];
                        if (permissionSupportedSet.indexOf(permission) >= 0) {
                            if (!permissionRequestSets[j]) {
                                permissionRequestSets[j] = Array()
                            }
                            permissionRequestSets[j].push(permission)
                            continue outer;
                        }
                    }
                }
                console.log("Made permission sets " + JSON.stringify(permissionRequestSets) + " out of supported " + JSON.stringify(permissionsSupportedSets))

                var requestSetKeys = Object.keys(permissionRequestSets)
                if (requestSetKeys.length > 0) {
                    var permissionsToRequest = permissionRequestSets[requestSetKeys[0]]
                    console.log("Requesting permissions " + JSON.stringify(permissionsToRequest))
                    RadarPassivePlugin.requestPermissions(permissionsToRequest, {
                        success: function(permissionsGranted) {
                            console.log("Granted permissions " + JSON.stringify(permissionsGranted) + " of " + JSON.stringify(permissionsToRequest));
                            if (permissionsGranted.length > 0) {
                                RadarPassivePlugin.onAcquiredPermissions(permissionsGranted, {
                                    success: function () {
                                        console.log("Indicated to plugin that permissions " + JSON.stringify(permissionsGranted) + " have been acquired.")
                                    }
                                })
                            }


                            var checkAgain = requestSetKeys.slice(1).flatMap(function (key) { return permissionRequestSets[key] })
                            if (checkAgain.length > 0) {
                                checkPermissions(checkAgain);
                            }
                        },
                        error: function(message) {
                            console.log("Failed to get permissions " + permissionsToRequest + ": " + message)

                            var checkAgain = requestSetKeys.slice(1).flatMap(function (key) { return permissionRequestSets[key] })
                            if (checkAgain.length > 0) {
                                checkPermissions(checkAgain);
                            }
                        }
                    })
                }
            }
        })
    }
}
